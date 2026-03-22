package com.example.prototype.export

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object CsvTxtWriter {

    fun write(
        outputStream: OutputStream,
        rows: List<RowData>,
        metadata: Map<String, String>,
        exportType: ExportType,
        delimiter: Char = ';'
    ) {
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            // Записываем метаданные (строки, начинающиеся с #)
            metadata.forEach { (key, value) ->
                writer.write("# $key: $value\n")
            }
            writer.write("#\n")

            // Формируем заголовок столбцов в зависимости от exportType
            val headerColumns = mutableListOf("Time(sec)")
            when (exportType) {
                ExportType.RAW -> headerColumns.add("RawECG")
                ExportType.FILTERED -> headerColumns.add("FilteredECG")
                ExportType.BOTH -> {
                    headerColumns.add("RawECG")
                    headerColumns.add("FilteredECG")
                }
            }
            headerColumns.add("QRS")
            headerColumns.add("HeartRate")
            headerColumns.add("Temperature")
            headerColumns.add("Humidity")
            headerColumns.add("Pressure")

            writer.write(headerColumns.joinToString(delimiter.toString()) + "\n")

            // Записываем строки данных
            for (row in rows) {
                val values = mutableListOf<String>()
                values.add(formatDouble(row.timeSec, delimiter))
                when (exportType) {
                    ExportType.RAW -> values.add(row.raw.toString())
                    ExportType.FILTERED -> values.add(row.filtered.toString())
                    ExportType.BOTH -> {
                        values.add(row.raw.toString())
                        values.add(row.filtered.toString())
                    }
                }
                values.add(row.qrs.toString())
                values.add(row.hr?.toString() ?: "")
                values.add(row.temp?.let { formatFloat(it, delimiter) } ?: "")
                values.add(row.hum?.let { formatFloat(it, delimiter) } ?: "")
                values.add(row.pres?.let { formatFloat(it, delimiter) } ?: "")

                writer.write(values.joinToString(delimiter.toString()) + "\n")
            }
        }
    }

    private fun formatDouble(value: Double, delimiter: Char): String {
        val str = String.format("%.3f", value)
        // Для CSV (разделитель ';') используем запятую как десятичный разделитель
        return if (delimiter == ';') str.replace('.', ',') else str
    }

    private fun formatFloat(value: Float, delimiter: Char): String {
        val str = String.format("%.1f", value)
        return if (delimiter == ';') str.replace('.', ',') else str
    }
}


object DatWriter {

    fun write(
        outputStream: OutputStream,
        rows: List<RowData>,
        metadata: Map<String, String>,
        exportType: ExportType
    ) {
        // Запись метаданных в виде текста
        val metaLines = metadata.entries.joinToString("\n") { "# ${it.key}: ${it.value}" }
        val header = "$metaLines\n#\n" // пустая строка с # для отделения
        outputStream.write(header.toByteArray(Charsets.UTF_8))

        // Разделитель – нулевой байт
        outputStream.write(0)

        // Бинарные данные
        val buffer = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.LITTLE_ENDIAN)
        // Заголовок: количество записей (int)
        buffer.putInt(rows.size)
        // Тип экспорта (можно добавить для самодостаточности)
        buffer.put(exportType.ordinal.toByte())

        for (row in rows) {
            buffer.putFloat(row.timeSec.toFloat())
            when (exportType) {
                ExportType.RAW -> buffer.putInt(row.raw)
                ExportType.FILTERED -> buffer.putInt(row.filtered)
                ExportType.BOTH -> {
                    buffer.putInt(row.raw)
                    buffer.putInt(row.filtered)
                }
            }
            buffer.putInt(row.qrs)
            buffer.putInt(row.hr ?: 0)
            buffer.putFloat(row.temp ?: 0f)
            buffer.putFloat(row.hum ?: 0f)
            buffer.putFloat(row.pres ?: 0f)
        }

        outputStream.write(buffer.array(), 0, buffer.position())
    }
}
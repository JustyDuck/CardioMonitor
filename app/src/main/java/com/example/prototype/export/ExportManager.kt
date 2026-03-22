package com.example.prototype.export

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.prototype.database.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ExportManager(
    private val context: Context,
    private val repository: Repository
) {

    private val dataCollector = DataCollector(repository)


    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun export(
        sessionId: Long,
        format: ExportFormat,
        exportType: ExportType,
        sessionMetadata: Map<String, String>
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Собираем данные
            val rows = dataCollector.collect(sessionId)
            if (rows.isEmpty()) {
                return@withContext ExportResult.Error("Нет данных для экспорта")
            }

            // Формируем имя файла
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateStr = dateFormat.format(Date(timestamp))
            val filename = "session_${sessionId}_$dateStr.${format.name.lowercase()}"

            // Определяем MIME-тип
            val mimeType = when (format) {
                ExportFormat.CSV -> "text/csv"
                ExportFormat.TXT -> "text/plain"
                ExportFormat.DAT -> "application/octet-stream"
            }

            // Создаём файл
            val fileInfo = FileHelper.createFileInDownloads(context, filename, mimeType)
                ?: return@withContext ExportResult.Error("Не удалось создать файл")

            val (uri, outputStream) = fileInfo

            // Записываем данные в зависимости от формата
            when (format) {
                ExportFormat.CSV, ExportFormat.TXT -> {
                    CsvTxtWriter.write(
                        outputStream = outputStream,
                        rows = rows,
                        metadata = sessionMetadata,
                        exportType = exportType,
                        delimiter = if (format == ExportFormat.CSV) ';' else '\t'
                    )
                }
                ExportFormat.DAT -> {
                    DatWriter.write(
                        outputStream = outputStream,
                        rows = rows,
                        metadata = sessionMetadata,
                        exportType = exportType
                    )
                }
            }

            outputStream.close()
            ExportResult.Success(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult.Error("Ошибка экспорта: ${e.message}")
        }
    }
}
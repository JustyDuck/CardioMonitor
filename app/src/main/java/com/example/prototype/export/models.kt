package com.example.prototype.export

import android.net.Uri

enum class ExportFormat {
    CSV,
    TXT,
    DAT
}


enum class ExportType {
    RAW,        // только сырые значения
    FILTERED,   // только фильтрованные
    BOTH        // оба
}


data class RowData(
    val timeSec: Double,           // относительное время от начала сеанса (секунды)
    val raw: Int,                   // сырое ЭКГ
    val filtered: Int,              // фильтрованное ЭКГ
    val qrs: Int,                   // флаг QRS (0 или 1)
    val hr: Int?,                   // пульс (уд/мин) или null
    val temp: Float?,               // температура
    val hum: Float?,                // влажность
    val pres: Float?                // давление
)


sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
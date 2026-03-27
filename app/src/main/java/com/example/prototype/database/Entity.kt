package com.example.prototype.database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    var endTime: Long? = null,
    val note: String? = null,
    val sessionType: String? = null,
    val plannedDurationSeconds: Int? = null

)

@Entity(
    tableName = "ecg_points",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EcgPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val rawValue: Int,
    val filteredValue: Int,
    val qrsPoint: Int
)

@Entity(
    tableName = "heart_rates",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class HeartRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val value: Int
)


@Entity(
    tableName = "weather_data",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WeatherData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val temperature: Float,
    val humidity: Float,
    val pressure: Float
)

@Entity(
    tableName = "rr_intervals",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionId"], name = "idx_rr_intervals_sessionId")]
)
data class RRInterval(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val index: Int,                 // порядковый номер интервала в сеансе
    val rrMs: Int,                  // длительность интервала в миллисекундах
    val quality: Int,               // 0 – артефакт, 1 – качественный
    val rPeakIndex: Long            // индекс точки в ecg_points (id), где обнаружен R
)

@Entity(
    tableName = "qrs_complexes",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class QrsComplex(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val index: Int,                     // порядковый номер комплекса
    val qrsStartIndex: Long,            // id точки начала QRS в ecg_points
    val qrsEndIndex: Long,              // id точки конца QRS
    val rPeakIndex: Long,               // точный индекс R-зубца (локальный максимум)
    val widthMs: Int?,                  // ширина QRS (в мс)
    val amplitude: Int?,                // амплитуда (разница между R и базовой линией)
    val quality: Int                    // 0 – артефакт, 1 – нормальный
)

@Entity(
    tableName = "wave_annotations",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WaveAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val complexIndex: Int,          // индекс QRS-комплекса, к которому относится
    val type: String,               // 'P' или 'T'
    val startIndex: Long,           // id точки начала зубца
    val endIndex: Long,             // id точки конца зубца
    val peakIndex: Long,            // id точки пика зубца
    val amplitude: Int,             // амплитуда (от изолинии)
    val quality: Int                // 0 – артефакт, 1 – нормальный
)

@Entity(tableName = "session_analysis")
data class SessionAnalysis(
    @PrimaryKey val sessionId: Long,    // ссылка на сессию (один к одному)
    val avgHr: Int,                     // средний пульс
    val sdnn: Int,                      // стандартное отклонение RR (мс)
    val rmssd: Int,                     // квадратный корень из среднего квадрата разностей RR (мс)
    val pnn50: Int,                     // процент соседних RR, отличающихся >50 мс
    val minHr: Int,                     // минимальный пульс
    val maxHr: Int,                     // максимальный пульс
    val bradycardiaEpisodes: Int,       // количество эпизодов брадикардии
    val tachycardiaEpisodes: Int,       // количество эпизодов тахикардии
    val prematureCount: Int,            // количество экстрасистол
    val atrialFibrillation: Boolean,    // подозрение на фибрилляцию предсердий
    val avgQrsWidth: Int?,              // средняя ширина QRS (мс)
    val overallQuality: Int,            // общее качество сигнала (0-100)
    val analysisDate: Long              // время анализа (timestamp)
)

@Entity(
    tableName = "annotations",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Annotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startIndex: Long,           // id точки начала события в ecg_points
    val endIndex: Long,             // id точки конца события
    val type: String,               // тип события: 'BRADY', 'TACHY', 'PVC', 'PAC', 'PAUSE', 'AFIB'
    val description: String,        // текстовое описание
    val severity: Int,              // 0-2 (низкая/средняя/высокая)
    val additionalData: String?     // JSON-подобные дополнительные параметры
)
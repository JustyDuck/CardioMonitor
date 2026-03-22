package com.example.prototype.viewmodels

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototype.bluetooth.BleManager
import com.example.prototype.database.AppDatabase
import com.example.prototype.database.Repository
import com.example.prototype.database.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.prototype.database.EcgPoint
import com.example.prototype.database.HeartRate
import com.example.prototype.database.WeatherData
import com.example.prototype.export.ExportFormat
import com.example.prototype.export.ExportManager
import com.example.prototype.export.ExportResult
import com.example.prototype.export.ExportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.firstOrNull


class EcgViewModel(application: Application) : AndroidViewModel(application) {
    private val bleManager = BleManager(application)
    private val db = AppDatabase.getInstance(getApplication())
    private val repository = Repository(db)

    private val _selectedSessionType = MutableStateFlow("rest") // rest / stress
    val selectedSessionType: StateFlow<String> = _selectedSessionType

    private val _plannedDurationMinutes = MutableStateFlow(0) //  0 = не ограничено
    val plannedDurationMinutes: StateFlow<Int> = _plannedDurationMinutes

    private val _exportResultState = MutableStateFlow<ExportResult?>(null)
    val exportResultState: StateFlow<ExportResult?> = _exportResultState

    private val exportManager = ExportManager(getApplication(), repository)
    val connectionState = bleManager.connectionState
    val receivedData = bleManager.receivedData
    val weatherData = bleManager.weatherData
    val ecgData = bleManager.ecgData
    val heartRate = bleManager.heartRate
    val electrodeStatus = bleManager.electrodeStatus


    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private var currentSessionId: Long? = null

    val sessions: Flow<List<Session>> = repository.getAllSessions()

    init {
        //Сохранение экг данных
        viewModelScope.launch {
            try {
                bleManager.newEcgSample.collect { (raw, filtered, qrs) ->
                    Log.d("EcgViewModel", "Sample received: raw=$raw, filtered=$filtered, qrs=$qrs, isRecording=${_isRecording.value}, sessionId=$currentSessionId")
                    if (_isRecording.value) {
                        currentSessionId?.let { sessionId ->
                            repository.saveEcgPoint(sessionId, System.currentTimeMillis(), raw, filtered, qrs)
                        }
                    }
                }
            } catch (e: Exception) {

            }
        }

        viewModelScope.launch {
            try {
                bleManager.newHeartRate.collect { pulse ->
                    if (_isRecording.value) {
                        currentSessionId?.let { sessionID ->
                            repository.saveHeartRate(sessionID, System.currentTimeMillis(), pulse)
                        }
                    }
                }
            } catch (e:Exception){

            }
        }

        viewModelScope.launch {
            try {
                bleManager.newWeatherData.collect{(temp, hum, pres)->
                    if (_isRecording.value){
                        currentSessionId?.let{ sessionID ->
                            repository.saveWeatherData(sessionID, System.currentTimeMillis(), temp, hum, pres)

                        }
                    }
                }

            }catch (e:Exception){

            }

        }
    }

    fun startRecording() {
        viewModelScope.launch {
            val plannedSec = if (_plannedDurationMinutes.value > 0)
                _plannedDurationMinutes.value * 60
            else
                null
            val sessionId = repository.startSession(
                startTime = System.currentTimeMillis(),
                sessionType = _selectedSessionType.value,
                plannedDurationSeconds = plannedSec
            )
            Log.d("EcgViewModel", "Session started with id $sessionId, isRecording set to true")
            currentSessionId = sessionId
            _isRecording.value = true

            plannedSec?.let { duration ->
                launchTimer(duration * 1000L)
            }

        }
    }

    private fun launchTimer(delayMillis: Long) {
        viewModelScope.launch {
            delay(delayMillis)
            // Проверяем, что запись всё ещё идёт и это та же сессия (на случай ручной остановки раньше)
            if (_isRecording.value) {
                stopRecording()
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                repository.finishSession(sessionId, System.currentTimeMillis())
            }
            currentSessionId = null
            _isRecording.value = false
        }
    }


    fun setSessionType(type: String) {
        _selectedSessionType.value = type
    }

    fun setPlannedDuration(minutes: Int) {
        _plannedDurationMinutes.value = minutes
    }


    init {
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                when (state) {
                    BleManager.ConnectionState.DISCONNECT -> {
                        // Обработка отключения
                    }
                    BleManager.ConnectionState.ERROR -> {
                        // Обработка ошибки
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect() // Автоматическое отключение при уничтожении ViewModel
    }

    fun connectToDevice() {
        bleManager.startScan()
    }

    fun disconnect() {
        bleManager.disconnect()
    }


    fun getEcgPointsForSession(sessionId: Long): Flow<List<EcgPoint>> = repository.getEcgPointsForSession(sessionId)
    fun getHeartRatesForSession(sessionId: Long): Flow<List<HeartRate>> = repository.getHeartRatesForSession(sessionId)
    fun getWeatherForSession(sessionId: Long): Flow<List<WeatherData>> = repository.getWeatherForSession(sessionId)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun exportSession(sessionId: Long, format: ExportFormat, type: ExportType) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId).firstOrNull()
            if (session == null) {
                _exportResultState.value = ExportResult.Error("Сессия не найдена")
                return@launch
            }
            val metadata = mapOf(
                "Session ID" to session.id.toString(),
                "Start time" to session.startTime.toString(),
                "End time" to (session.endTime?.toString() ?: "null"),
                "Type" to (session.sessionType ?: "unknown"),
                "Planned duration" to (session.plannedDurationSeconds?.toString() ?: "none")
            )
            val result = exportManager.export(sessionId, format, type, metadata)
            _exportResultState.value = result
        }
    }

    fun clearExportResult() {
        viewModelScope.launch {
            _exportResultState.value = null
        }
    }



}
package com.example.prototype.viewmodels

import android.app.Application
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
import kotlinx.coroutines.delay

class EcgViewModel(application: Application) : AndroidViewModel(application) {
    private val bleManager = BleManager(application)
    private val db = AppDatabase.getInstance(getApplication())
    private val repository = Repository(db)

    private val _selectedSessionType = MutableStateFlow("rest") // rest / stress
    val selectedSessionType: StateFlow<String> = _selectedSessionType

    private val _plannedDurationMinutes = MutableStateFlow(0) //  0 = не ограничено
    val plannedDurationMinutes: StateFlow<Int> = _plannedDurationMinutes

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
        viewModelScope.launch {
            // Подписка на новые точки ЭКГ (после доработки BleManager)
            bleManager.newEcgPoint.collect { (raw, filtered) ->
                if (_isRecording.value) {
                    currentSessionId?.let { sessionId ->
                        repository.saveEcgPoint(sessionId, System.currentTimeMillis(), raw, filtered)
                    }
                }
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
}
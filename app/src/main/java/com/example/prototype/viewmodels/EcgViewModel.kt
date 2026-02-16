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

class EcgViewModel(application: Application) : AndroidViewModel(application) {
    private val bleManager = BleManager(application)
    private val db = AppDatabase.getInstance(getApplication())
    private val repository = Repository(db)

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
            val sessionId = repository.startSession(System.currentTimeMillis())
            currentSessionId = sessionId
            _isRecording.value = true
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
}
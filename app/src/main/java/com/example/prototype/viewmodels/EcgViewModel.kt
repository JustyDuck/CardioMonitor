package com.example.prototype.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototype.bluetooth.BleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class EcgViewModel(application: Application) : AndroidViewModel(application) {
    private val bleManager = BleManager(application)

    val connectionState = bleManager.connectionState
    val receivedData = bleManager.receivedData
    val weatherData = bleManager.weatherData
    val ecgData = bleManager.ecgData
    val heartRate = bleManager.heartRate
    val electrodeStatus = bleManager.electrodeStatus






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
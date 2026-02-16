package com.example.prototype.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

data class EcgDataPoint(
    val value: Int,
    val timestamp: Long,
    val heartRate: Int
)

class BleManager(private val context: Context) {
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    enum class ConnectionState {
        DISCONNECT,
        CONNECTING,
        CONNECTED,
        SCANNING,
        ERROR
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECT)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _receivedData = MutableStateFlow<String?>(null)
    val receivedData: StateFlow<String?> = _receivedData

    private val _weatherData = MutableStateFlow(Triple(0f, 0f, 0f))
    val weatherData: StateFlow<Triple<Float, Float, Float>> = _weatherData

    private val _rawEcgData = MutableStateFlow<List<Int>>(emptyList())
    val rawEcgData: StateFlow<List<Int>> = _rawEcgData

    private val _filEcgData = MutableStateFlow<List<Int>>(emptyList())
    val ecgData: StateFlow<List<Int>> = _filEcgData

    private val _newEcgPoint = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 64)
    val newEcgPoint: SharedFlow<Pair<Int, Int>> = _newEcgPoint

    private val _newHeartRate = MutableSharedFlow<Int>(extraBufferCapacity = 10)
    val newHeartRate: SharedFlow<Int> = _newHeartRate

    private val _newWeatherData = MutableSharedFlow<Triple<Float, Float, Float>>(extraBufferCapacity = 5)
    val newWeatherData: SharedFlow<Triple<Float, Float, Float>> = _newWeatherData



    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate

    private val _electrodeStatus = MutableStateFlow("?")
    val electrodeStatus: StateFlow<String> = _electrodeStatus

    companion object {
        val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Получаем имя устройства из ScanRecord (не требует BLUETOOTH_CONNECT)
            val deviceName = result.scanRecord?.deviceName ?: result.device?.name ?: "Unknown"
            val deviceAddress = result.device?.address ?: "Unknown"

            Log.d("BleManager", "Найдено устройство: $deviceName ($deviceAddress)")

            // Проверяем имя устройства (ESP32 использует "CardioMonitor")
            if (deviceName.contains("CardioMonitor", ignoreCase = true)) {
                Log.d("BleManager", "Найдено целевое устройство: $deviceName")
                stopScan()
                connectToDevice(result.device)
                return
            }

            // Дополнительная проверка по UUID сервиса
            val serviceUuids = result.scanRecord?.serviceUuids
            serviceUuids?.let { uuids ->
                uuids.forEach { uuid ->
                    Log.d("BleManager", "UUID устройства: $uuid")
                    if (uuid.uuid.toString().contains("FFE0", ignoreCase = true)) {
                        Log.d("BleManager", "Найдено устройство по UUID: $deviceName")
                        stopScan()
                        connectToDevice(result.device)
                        return
                    }
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            // Этот метод не используется в нашем случае, но нужно его реализовать
            // Используем ScanRecord для получения имени без разрешения BLUETOOTH_CONNECT
            try {
                results.forEach { result ->
                    val deviceName = result.scanRecord?.deviceName ?: "Unknown"
                    Log.d("BleManager", "Пакетное сканирование: $deviceName")
                }
            } catch (e: Exception) {
                Log.e("BleManager", "Ошибка в onBatchScanResults: ${e.message}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleManager", "Ошибка сканирования: $errorCode")
            when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> {
                    Log.e("BleManager", "Сканирование уже запущено")
                }
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    Log.e("BleManager", "Ошибка регистрации приложения")
                }
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> {
                    Log.e("BleManager", "Внутренняя ошибка сканирования")
                }
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> {
                    Log.e("BleManager", "Функция сканирования не поддерживается")
                }
                else -> {
                    Log.e("BleManager", "Неизвестная ошибка сканирования: $errorCode")
                }
            }
            _connectionState.value = ConnectionState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        try {
            if (bluetoothAdapter == null) {
                Log.e("BleManager", "Bluetooth адаптер не найден")
                _connectionState.value = ConnectionState.ERROR
                return
            }

            if (bluetoothAdapter?.isEnabled != true) {
                Log.e("BleManager", "Bluetooth выключен")
                _connectionState.value = ConnectionState.ERROR
                return
            }

            if (bluetoothLeScanner == null) {
                Log.e("BleManager", "Bluetooth сканер не доступен")
                _connectionState.value = ConnectionState.ERROR
                return
            }

            Log.d("BleManager", "Начинаем сканирование BLE устройств...")
            _connectionState.value = ConnectionState.SCANNING

            // Вариант 1: Фильтр по имени устройства (из ScanRecord)
            val filters = listOf(
                ScanFilter.Builder()
                    .setDeviceName("CardioMonitor")
                    .build()
            )

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0) // Немедленные результаты
                .build()

            bluetoothLeScanner.startScan(filters, settings, scanCallback)

            // Останавливаем сканирование через 10 секунд, если ничего не найдено
            android.os.Handler(context.mainLooper).postDelayed({
                if (_connectionState.value == ConnectionState.SCANNING) {
                    Log.w("BleManager", "Таймаут сканирования")
                    stopScan()
                    _connectionState.value = ConnectionState.ERROR
                }
            }, 10000)

        } catch (e: SecurityException) {
            Log.e("BleManager", "Ошибка разрешений: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        } catch (e: Exception) {
            Log.e("BleManager", "Ошибка при сканировании: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            Log.d("BleManager", "Сканирование остановлено")
        } catch (e: Exception) {
            Log.e("BleManager", "Ошибка при остановке сканирования: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice?) {
        device?.let {
            Log.d("BleManager", "Подключаемся к устройству: ${device.name ?: device.address}")
            _connectionState.value = ConnectionState.CONNECTING

            // Закрываем предыдущее подключение, если есть
            bluetoothGatt?.close()

            // Подключаемся с autoConnect = false для немедленного подключения
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

            // Таймаут подключения
            android.os.Handler(context.mainLooper).postDelayed({
                if (_connectionState.value == ConnectionState.CONNECTING) {
                    Log.w("BleManager", "Таймаут подключения")
                    disconnect()
                    _connectionState.value = ConnectionState.ERROR
                }
            }, 15000)
        } ?: run {
            Log.e("BleManager", "Устройство для подключения не указано")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BleManager", "Устройство подключено, статус: $status")

                    // Даем время на установление соединения
                    android.os.Handler(context.mainLooper).postDelayed({
                        _connectionState.value = ConnectionState.CONNECTED
                        // Запрашиваем увеличение MTU для больших пакетов
                        val success = gatt.requestMtu(512)
                        Log.d("BleManager", "Запрос MTU: $success")
                    }, 500)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BleManager", "Устройство отключено, статус: $status")
                    _connectionState.value = ConnectionState.DISCONNECT
                    gatt.close()
                    bluetoothGatt = null
                }
                else -> {
                    Log.d("BleManager", "Неизвестное состояние подключения: $newState")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("BleManager", "MTU изменен: $mtu, статус: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleManager", "Обнаруживаем сервисы...")
                gatt.discoverServices()
            } else {
                Log.e("BleManager", "Ошибка изменения MTU: $status")
                // Продолжаем без MTU
                gatt.discoverServices()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleManager", "Сервисы обнаружены успешно")

                // Логируем все найденные сервисы
                gatt.services?.forEach { service ->
                    Log.d("BleManager", "Найден сервис: ${service.uuid}")
                    service.characteristics?.forEach { characteristic ->
                        Log.d("BleManager", "  Характеристика: ${characteristic.uuid}")
                    }
                }

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e("BleManager", "Сервис $SERVICE_UUID не найден")
                    // Пробуем найти сервис по части UUID
                    gatt.services?.forEach { s ->
                        if (s.uuid.toString().contains("FFE0")) {
                            Log.d("BleManager", "Найден похожий сервис: ${s.uuid}")
                            val characteristic = s.getCharacteristic(CHARACTERISTIC_UUID)
                            setupCharacteristic(gatt, characteristic)
                            return
                        }
                    }
                } else {
                    Log.d("BleManager", "Сервис найден: $SERVICE_UUID")
                    val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                    setupCharacteristic(gatt, characteristic)
                }
            } else {
                Log.e("BleManager", "Ошибка обнаружения сервисов: $status")
            }
        }

        @SuppressLint("MissingPermission")
        private fun setupCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.let {
                Log.d("BleManager", "Настройка характеристики: ${characteristic.uuid}")

                // Включаем уведомления
                val enabled = gatt.setCharacteristicNotification(characteristic, true)
                Log.d("BleManager", "Уведомления включены: $enabled")

                // Настраиваем дескриптор для уведомлений
                val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                descriptor?.let { desc ->
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(desc)
                    Log.d("BleManager", "Дескриптор настроен")
                } ?: run {
                    Log.e("BleManager", "Дескриптор не найден")
                }
            } ?: run {
                Log.e("BleManager", "Характеристика не найдена")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            val dataString = String(data, Charsets.UTF_8)
            Log.d("BleManager", "Получены данные: $dataString")
            _receivedData.value = dataString
            parseData(dataString)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleManager", "Дескриптор успешно записан")
            } else {
                Log.e("BleManager", "Ошибка записи дескриптора: $status")
            }
        }
    }

    private fun parseData(dataString: String) {
        try {
            val cleanString = dataString
                .replace("\n", "")
                .replace("\r", "")
                .replace("\u0000", "")
                .trim()

            Log.d("BleManager", "Очищенная строка: '$cleanString'")

            when {
                cleanString.startsWith("TEMP:") -> {
                    parseWeatherData(cleanString)
                }
                cleanString.startsWith("RAWECG:") -> {
                    parseEcgData(cleanString)
                }
                cleanString.startsWith("STATUS:") -> {
                    parseStatusData(cleanString)
                }
                else -> {
                    Log.w("BleManager", "Неизвестный формат данных: $cleanString")
                }
            }

        } catch (e: Exception) {
            Log.e("BleManager", "Ошибка парсинга: ${e.message}", e)
        }
    }

    private fun parseWeatherData(dataString: String) {
        val pairs = dataString.split(",")
        var temp = 0f
        var hum = 0f
        var pres = 0f

        pairs.forEach { pair ->
            val keyValue = pair.split(":")
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim()
                val floatValue = value.toFloatOrNull()

                when (key) {
                    "TEMP" -> temp = floatValue ?: 0f
                    "HUM" -> hum = floatValue ?: 0f
                    "PRES" -> pres = floatValue ?: 0f
                }
            }
        }
        _weatherData.value = Triple(temp, hum, pres)
        _newWeatherData.tryEmit(Triple(temp, hum, pres))

    }

    private fun parseEcgData(dataString: String) {
        val pairs = dataString.split(",")
        var rawValue = 0
        var filteredValue = 0
        var pulse = 0

        pairs.forEach { pair ->
            val keyValue = pair.split(":")
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim()
                when (key) {
                    "RAWECG" -> rawValue = value.toIntOrNull() ?: 0
                    "FILECG" -> filteredValue = value.toIntOrNull() ?: 0
                    "PULSE" -> pulse = value.toIntOrNull() ?: 0
                }
            }
        }

        updateList(_rawEcgData, rawValue)
        updateList(_filEcgData, filteredValue)

        _newEcgPoint.tryEmit(rawValue to filteredValue)

        if (pulse != 0) {
            _heartRate.value = pulse
            _newHeartRate.tryEmit(pulse)
        }
    }

    private fun updateList(stateFlow: MutableStateFlow<List<Int>>, newValue: Int) {
        val currentList = stateFlow.value.toMutableList()
        if (currentList.size >= 1000) {
            currentList.removeAt(0)
        }
        currentList.add(newValue)
        stateFlow.value = currentList
    }

    private fun parseStatusData(dataString: String) {
        when {
            dataString.contains("STATUS:GOOD") -> {
                _electrodeStatus.value = "Норма"
                Log.d("BleManager", "Статус электродов: Норма")
            }
            dataString.contains("STATUS:POOR") -> {
                _electrodeStatus.value = "Плохой контакт"
                Log.d("BleManager", "Статус электродов: Плохой контакт")
            }
            else -> {
                Log.w("BleManager", "Неизвестный статус: $dataString")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _connectionState.value = ConnectionState.DISCONNECT
            Log.d("BleManager", "Отключено")
        } catch (e: Exception) {
            Log.e("BleManager", "Ошибка при отключении: ${e.message}")
        }
    }

    fun clearEcgData() {
        _rawEcgData.value = emptyList()
        _filEcgData.value = emptyList()
    }
}
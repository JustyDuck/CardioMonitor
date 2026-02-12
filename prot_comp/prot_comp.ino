#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Wire.h>
#include <Adafruit_BME280.h>

// UUID сервиса и характеристики
#define SERVICE_UUID "0000FFE0-0000-1000-8000-00805F9B34FB"
#define CHARACTERISTIC_UUID "0000FFE1-0000-1000-8000-00805F9B34FB"


// Датчик BME280
Adafruit_BME280 bme;

// BLE переменные
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// Пины AD8232
#define LO_PLUS 5     // LO+
#define LO_MINUS 17   // LO-
#define ECG_OUTPUT 4  // OUTPUT ЭКГ

// Переменные для ЭКГ
const int ECG_BUFFER_SIZE = 100;
const int PULSE_INTERVAL = 15000;
int ecgBuffer[ECG_BUFFER_SIZE];
int filteredEcgBuffer[ECG_BUFFER_SIZE];
int qrsDetectionBuffer[ECG_BUFFER_SIZE];
int bufferIndex = 0;
int qrsState = 0;
int pulseCount = 0;
int calculatedPulse = 0;
float detectionThreshold = 0;
unsigned long lastPulseCalculation = 0;
unsigned long lastEcgProcess = 0;
const int ECG_PROCESS_INTERVAL = 20;  // 50 Гц

// Таймеры
unsigned long previousMillis = 0;
const long DATA_INTERVAL = 1000;    // Интервал отправки (1 секунда)
const long ECG_SEND_INTERVAL = 20;  // Отправка ЭКГ каждые 20мс

// Статус электродов
bool electrodeContactGood = true;
unsigned long lastElectrodeCheck = 0;
const long ELECTRODE_CHECK_INTERVAL = 1000; // Проверка каждую секунду

// Прототипы функций ЭКГ
void initializeEcgProcessing();
void calibrateDetectionThreshold();
float exponentialFilter(float newValue);
void processEcgData();
bool checkElectrodeContact();
void handlePoorSignal();
void updateEcgBuffers(int ecgValue);
void detectQrsComplex();
void calculatePulse();

// Callback для подключения/отключения
class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    Serial.println("Устройство подключено");
  };

  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    Serial.println("Устройство отключено");
    pServer->getAdvertising()->start();
  }
};


void setup() {
  Serial.begin(115200);

  if (!bme.begin(0x76)) {
    Serial.println("Не удалось найти BME280 датчик!");
    while (1)
      ;
  }

  // Настройка пинов ЭКГ
  pinMode(LO_PLUS, INPUT_PULLUP);
  pinMode(LO_MINUS, INPUT_PULLUP);
  pinMode(ECG_OUTPUT, INPUT);

  initializeEcgProcessing();

  // Инициализация BLE
  BLEDevice::init("CardioMonitor");

  // Создание BLE сервера
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Создание BLE сервиса
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Создание BLE характеристики
  pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY | BLECharacteristic::PROPERTY_INDICATE);

  // Добавление дескриптора
  pCharacteristic->addDescriptor(new BLE2902());

  // Запуск сервиса
  pService->start();

  // Начало рекламы
  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  Serial.println("Сервер BLE запущен. Ожидание подключения...");
}


void loop() {
  // Обработка подключения/отключения
  if (!deviceConnected && oldDeviceConnected) {
    delay(500);  // даем время для завершения подключения
    pServer->startAdvertising();
    oldDeviceConnected = deviceConnected;
  }

  if (deviceConnected && !oldDeviceConnected) {
    oldDeviceConnected = deviceConnected;
  }

  // Обработка данных ЭКГ
  processEcgData();

  unsigned long currentMillis = millis();
  if (currentMillis - lastElectrodeCheck >= ELECTRODE_CHECK_INTERVAL){
    lastElectrodeCheck = currentMillis;
    bool currentContact = checkElectrodeContact();

    if (currentContact != electrodeContactGood){
      electrodeContactGood = currentContact;

      if (deviceConnected){
        char statusString[30];
        sprintf(statusString, "STATUS:%s", electrodeContactGood ? "GOOD" : "POOR");
        pCharacteristic->setValue(statusString);
        pCharacteristic->notify();
      }
    }
  }

  if (deviceConnected) {
    static unsigned lastEcgSend = 0;
    if (currentMillis - lastEcgSend >= ECG_SEND_INTERVAL) {
      lastEcgSend = currentMillis;

      //Чтение ЭКГ
      int rawEcg = analogRead(ECG_OUTPUT);
      float filtredEcg = exponentialFilter(rawEcg);
      int ecgValue = filtredEcg;

      if (electrodeContactGood){
        char ecgString[30];
        sprintf(ecgString, "ECG:%d,PULSE:%d", ecgValue, calculatedPulse);
        //Отправка данных
        pCharacteristic->setValue(ecgString);
        pCharacteristic->notify();

        Serial.println("ECG:");
        Serial.println(ecgString);
      }

    }

    if (currentMillis - previousMillis >= DATA_INTERVAL){
      previousMillis = currentMillis;

      //Чтение данных BMP280
      float temperature = bme.readTemperature();
      float humidity = bme.readHumidity();
      float pressure = bme.readPressure() / 100.0F;

      //Формирование строки данных BME
      char weatherString[50];
      sprintf(weatherString, "TEMP:%.1f,HUM:%.1f,PRES:%.1f", temperature, humidity, pressure);

      //отправка данных
      pCharacteristic->setValue(weatherString);
      pCharacteristic->notify();

      Serial.println("Weather: ");
      Serial.println(weatherString);
    }
  }
}


// Инициализация буферов
void initializeEcgProcessing() {
  // Инициализация буферов
  for (int i = 0; i < ECG_BUFFER_SIZE; i++) {
    ecgBuffer[i] = 0;
    filteredEcgBuffer[i] = 0;
    qrsDetectionBuffer[i] = 0;
  }

  // Калибровка порога обнаружения
  calibrateDetectionThreshold();
}

void calibrateDetectionThreshold() {
  Serial.println("Калибровка порога обнаружения ЭКГ...");
  float maxValue = 0;

  for (int i = 0; i < 1000 - 1; i++) {
    float rawValue = analogRead(ECG_OUTPUT);
    float filteredValue = exponentialFilter(rawValue) - rawValue;

    if (filteredValue > maxValue) {
      maxValue = filteredValue;
    }
  }

  detectionThreshold = maxValue * 10;
  Serial.print("Порог обнаружения: ");
  Serial.println(detectionThreshold);
}

float exponentialFilter(float newValue) {
  static float filteredValue = 0;
  filteredValue += (newValue - filteredValue) / 10 * 7;  // Коэффициент сглаживания
  return filteredValue;
}

void processEcgData() {
  if (millis() - lastEcgProcess < ECG_PROCESS_INTERVAL) {
    return;
  }
  lastEcgProcess = millis();

  // Чтение и обработка данных ЭКГ
  int rawEcg = analogRead(ECG_OUTPUT);
  float filteredEcg = exponentialFilter(rawEcg);
  int ecgValue = rawEcg - (int)filteredEcg;

  // Обновление буферов
  updateEcgBuffers(ecgValue);

  // Обнаружение QRS комплексов
  detectQrsComplex();

  // Расчет пульса
  calculatePulse();

}

bool checkElectrodeContact() {
    // Проверяем оба электрода
    bool loPlusGood = digitalRead(LO_PLUS) == 0;
    bool loMinusGood = digitalRead(LO_MINUS) == 0;
    
    // Оба электрода должны иметь хороший контакт
    return loPlusGood && loMinusGood;
}

void handlePoorSignal() {
    // Теперь статус отправляется отдельно
    if (deviceConnected) {
        pCharacteristic->setValue("STATUS:POOR");
        pCharacteristic->notify();
    }
    Serial.println("Ошибка: плохой контакт электродов");
}

void updateEcgBuffers(int ecgValue) {
  // Сдвиг буферов
  for (int i = 0; i < ECG_BUFFER_SIZE - 1; i++) {
    ecgBuffer[i] = ecgBuffer[i + 1];
    filteredEcgBuffer[i] = filteredEcgBuffer[i + 1];
    qrsDetectionBuffer[i] = qrsDetectionBuffer[i + 1];
  }

  // Добавление новых значений
  ecgBuffer[ECG_BUFFER_SIZE - 1] = ecgValue * 2;
  filteredEcgBuffer[ECG_BUFFER_SIZE - 1] = ecgValue;
  qrsDetectionBuffer[ECG_BUFFER_SIZE - 1] = ecgValue * 2; 
}

void detectQrsComplex() {
  int currentValue = qrsDetectionBuffer[ECG_BUFFER_SIZE - 1];

  if (currentValue > detectionThreshold && qrsState == 0) {
    qrsState = 1;
    pulseCount++;
  } else if (currentValue > detectionThreshold && qrsState == 1) {
    qrsState = -1;
  } else {
    qrsState = 0;
  }
}

void calculatePulse() {
  if (millis() - lastPulseCalculation >= PULSE_INTERVAL) {
    // ИСПРАВЛЕНО: правильный расчет пульса
    calculatedPulse = (pulseCount * 60000) / PULSE_INTERVAL;
    pulseCount = 0;

    Serial.print("Пульс: ");
    Serial.println(calculatedPulse);

    lastPulseCalculation = millis();
  }
}

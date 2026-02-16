package com.example.prototype.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prototype.bluetooth.BleManager
import com.example.prototype.components.EcgGraph
import com.example.prototype.viewmodels.EcgViewModel

@Composable
fun MainScreen(
    viewModel: EcgViewModel = viewModel(),
    onRequestPermissions: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val weatherData by viewModel.weatherData.collectAsState()
    val ecgData by viewModel.ecgData.collectAsState()
    val heartRate by viewModel.heartRate.collectAsState()
    val electrodeStatus by viewModel.electrodeStatus.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()


    LaunchedEffect(Unit) {
        onRequestPermissions()
        onEnableBluetooth()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Кардиомонитор",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ConnectionSection(connectionState, viewModel, onRequestPermissions)

            Spacer(modifier = Modifier.height(24.dp))


            // График ЭКГ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
            ) {
                EcgGraph(
                    ecgValues = ecgData,
                    heartRate = heartRate,
                    electrodeStatus = electrodeStatus,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            WeatherSection(weatherData)

            Spacer(modifier = Modifier.height(24.dp))

        }
    }
}


@Composable
fun ConnectionSection(
    state: BleManager.ConnectionState,
    viewModel: EcgViewModel,
    onRequestPermissions: () -> Unit = {}
) {
    Card(modifier = Modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                BleManager.ConnectionState.DISCONNECT -> {
                    Text(
                        text = "Устройство не подключено",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            onRequestPermissions()
                            viewModel.connectToDevice()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Подключиться к устройству")
                    }
                }

                BleManager.ConnectionState.SCANNING -> {
                    Text("Поиск устройства...", modifier = Modifier.padding(bottom = 12.dp))
                    CircularProgressIndicator()
                }

                BleManager.ConnectionState.CONNECTING -> {
                    Text("Подключение...", modifier = Modifier.padding(bottom = 12.dp))
                    CircularProgressIndicator()
                }

                BleManager.ConnectionState.CONNECTED -> {
                    Text("Устройство подключено", modifier = Modifier.padding(bottom = 12.dp))
                    Icon(
                        androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = "Подключено",
                        tint = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                }

                BleManager.ConnectionState.ERROR -> {
                    Text(
                        text = "Ошибка подключения",
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            onRequestPermissions()
                            viewModel.connectToDevice()
                        }
                    ) {
                        Text("Повторить попытку")
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherSection(weatherData: Triple<Float, Float, Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Метеоданные",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Температура
                WeatherItem(
                    icon = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                    value = "${weatherData.first}°C",
                    label = "Температура"
                )

                // Влажность
                WeatherItem(
                    icon = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                    value = "${weatherData.second}%",
                    label = "Влажность"
                )

                // Давление
                WeatherItem(
                    icon = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                    value = "${weatherData.third} hPa",
                    label = "Давление"
                )
            }
        }
    }
}


@Composable
fun WeatherItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Иконка
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Отступ между иконкой и значением
        Spacer(modifier = Modifier.height(8.dp))

        // Значение (крупный текст)
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Отступ между значением и подписью
        Spacer(modifier = Modifier.height(4.dp))

        // Подпись (мелкий текст)
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
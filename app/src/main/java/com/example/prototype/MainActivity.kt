package com.example.prototype

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prototype.screens.HomeScreen
import com.example.prototype.screens.LoadingScreen
import com.example.prototype.viewmodels.EcgViewModel

class MainActivity : ComponentActivity() {

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkBluetoothEnabled()
        } else {
            // Обработка отказа
        }
    }

    // Launcher для включения Bluetooth
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Bluetooth включен
        } else {
            // Пользователь отказался включать Bluetooth
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            var isLoading by remember { mutableStateOf(true) }

            if (isLoading) {
                LoadingScreen(
                    onLoadingComplete = {
                        isLoading = false
                    }
                )
            } else {
                HomeScreen(
                    onRequestPermissions = { checkAndRequestPermissions() },
                    onEnableBluetooth = { if (arePermissionsGranted()){
                        enableBluetooth()
                    } }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!arePermissionsGranted()) {
            requestPermissions()
        } else {
            checkBluetoothEnabled()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        val basePermissions = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        ).all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return basePermissions &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            return basePermissions &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Для Android 12+ используем новые разрешения
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Для старых версий
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Всегда запрашиваем базовые разрешения
        permissionsToRequest.add(Manifest.permission.BLUETOOTH)
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)

        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == false) {
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }
}
package com.example.prototype.screens

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ModalDrawer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.WhitePoint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.prototype.R
import com.example.prototype.viewmodels.EcgViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(
    onRequestPermissions: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val viewModel: EcgViewModel = viewModel()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("EKG Monitor") }
            )
        },


        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(42.dp),
                tonalElevation = 0.dp
            ) {
                // Главный экран
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.home_icon),
                            contentDescription = "Главный экран",
                            modifier = Modifier.size(20.dp)

                        )
                    },
                    selected = currentRoute == "main_screen",
                    onClick = {
                        navController.navigate("main_screen") {
                            popUpTo("main_screen") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // История
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.history_icon),
                            contentDescription = "История",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selected = currentRoute == "database_screen",
                    onClick = {
                        navController.navigate("database_screen") {
                            popUpTo("main_screen") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        content = { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "main_screen",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("main_screen") {
                    MainScreen(
                        onRequestPermissions = onRequestPermissions,
                        onEnableBluetooth = onEnableBluetooth
                    )
                }
                composable("database_screen") {
                    DatabaseScreen(
                        onSessionClick = { sessionId ->
                            navController.navigate("session_detail/$sessionId")
                        }
                    )
                }
                composable("session_detail/{sessionId}") { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: 0L
                    SessionDetailScreen(sessionId = sessionId)
                }
            }
        }
    )
}



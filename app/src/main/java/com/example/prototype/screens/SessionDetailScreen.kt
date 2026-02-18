package com.example.prototype.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prototype.components.ZoomableEcgGraph
import com.example.prototype.viewmodels.EcgViewModel

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    viewModel: EcgViewModel = viewModel()
) {
    val ecgPoints by viewModel.getEcgPointsForSession(sessionId).collectAsState(initial = emptyList())

    if (ecgPoints.isEmpty()) {
        Text("Нет данных для этого сеанса", modifier = Modifier.padding(16.dp))
        return
    }

    val values = ecgPoints.map { it.filteredValue }
    val timestamps = ecgPoints.map { it.timestamp }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Детальный просмотр", modifier = Modifier.padding(16.dp))
        ZoomableEcgGraph(
            ecgValues = values,
            timestamps = timestamps,
            modifier = Modifier.weight(1f)
        )
    }
}
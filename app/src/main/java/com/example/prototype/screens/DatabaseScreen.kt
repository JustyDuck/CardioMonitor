package com.example.prototype.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prototype.database.Session
import com.example.prototype.viewmodels.EcgViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DatabaseScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: EcgViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            SessionCard(session = session, onClick = { onSessionClick(session.id) })
        }
    }
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                Text(
                    text = "Начало: ${dateFormat.format(Date(session.startTime))}",
                    style = MaterialTheme.typography.bodyLarge
                )
                session.endTime?.let { endTime ->
                    val duration = (endTime - session.startTime) / 1000 // секунды
                    Text(
                        text = "Длительность: $duration сек",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } ?: Text(
                    text = "Не завершён",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Можно добавить иконку, например стрелку
        }
    }
}
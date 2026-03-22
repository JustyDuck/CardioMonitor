package com.example.prototype.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prototype.database.Session
import com.example.prototype.export.ExportFormat
import com.example.prototype.export.ExportResult
import com.example.prototype.export.ExportType
import com.example.prototype.viewmodels.EcgViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DatabaseScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: EcgViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    val exportResult by viewModel.exportResultState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(exportResult) {
        exportResult?.let { result ->
            when (result) {
                is ExportResult.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Файл сохранён: ${result.uri.path ?: "в папке Downloads"}",
                            duration = SnackbarDuration.Long
                        )
                    }
                    viewModel.clearExportResult()
                }
                is ExportResult.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Ошибка: ${result.message}",
                            duration = SnackbarDuration.Long
                        )
                    }
                    viewModel.clearExportResult()
                }

                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    onExportClick = {
                        selectedSessionId = session.id
                        showExportDialog = true
                    }
                )
            }
        }
    }

    if (showExportDialog && selectedSessionId != null) {
        ExportDialog(
            sessionId = selectedSessionId!!,
            onDismiss = { showExportDialog = false },
            onExport = { format, type ->
                viewModel.exportSession(selectedSessionId!!, format, type)
            }
        )
    }
}

@Composable
fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    onExportClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                Text(
                    text = "Начало: ${dateFormat.format(Date(session.startTime))}",
                    style = MaterialTheme.typography.bodyLarge
                )
                session.endTime?.let { endTime ->
                    val duration = (endTime - session.startTime) / 1000
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
            IconButton(onClick = onExportClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Экспорт"
                )
            }
        }
    }
}

@Composable
fun ExportDialog(
    sessionId: Long,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, ExportType) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedType by remember { mutableStateOf(ExportType.BOTH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт данных") },
        text = {
            Column {
                Text("Формат файла:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExportFormat.values().forEach { format ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            RadioButton(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format }
                            )
                            Text(format.name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Данные ЭКГ:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExportType.values().forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text(
                                when (type) {
                                    ExportType.RAW -> "Сырые"
                                    ExportType.FILTERED -> "Фильтрованные"
                                    ExportType.BOTH -> "Оба"
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onExport(selectedFormat, selectedType)
                onDismiss()
            }) {
                Text("Экспортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
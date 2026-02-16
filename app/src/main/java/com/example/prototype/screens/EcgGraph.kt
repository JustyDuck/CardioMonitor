package com.example.prototype.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
fun EcgGraph(
    ecgValues: List<Int>,
    heartRate: Int,
    electrodeStatus: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ЭКГ График",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Пульс: $heartRate уд/мин",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }

            // Статус
            Text(
                text = "Статус электродов: $electrodeStatus",
                color = if (electrodeStatus == "Норма") Color.Green else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

//            // Информация о данных
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    text = "Точек: ${ecgValues.size}",
//                    fontSize = 10.sp,
//                    color = Color.Gray
//                )
//                ecgValues.lastOrNull()?.let { lastValue ->
//                    Text(
//                        text = "Текущее: $lastValue",
//                        fontSize = 10.sp,
//                        color = Color.Gray
//                    )
//                }
//            }

            // График
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(top = 8.dp)
            ) {
                if (ecgValues.isNotEmpty()) {
                    EcgCanvas(ecgValues = ecgValues)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ожидание данных ЭКГ...",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EcgCanvas(
    ecgValues: List<Int>,
    modifier: Modifier = Modifier
) {
    val displayValues = remember(ecgValues) {
        // Берем последние 200 точек и нормализуем
        val lastValues = ecgValues.takeLast(200)
        if (lastValues.isEmpty()) return@remember emptyList<Float>()

        val avg = lastValues.average()
        // Нормализуем относительно среднего
        lastValues.map { (it - avg).toFloat() }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (displayValues.isEmpty() || displayValues.size < 2) return@Canvas

        // Находим максимальное абсолютное значение для масштабирования
        val maxAbs = displayValues.maxOf { kotlin.math.abs(it) }
        if (maxAbs == 0f) return@Canvas

        val padding = 20f
        val graphHeight = size.height - 2 * padding
        val graphWidth = size.width - 2 * padding

        val path = Path()
        val stepX = graphWidth / (displayValues.size - 1)

        var x = padding

        for (i in displayValues.indices) {
            val normalizedValue = displayValues[i] / maxAbs // От -1 до 1
            val y = size.height / 2 - normalizedValue * (graphHeight / 2)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            x += stepX
        }

        // Центральная линия
        drawLine(
            start = Offset(padding, size.height / 2),
            end = Offset(size.width - padding, size.height / 2),
            color = Color.Gray.copy(alpha = 0.3f),
            strokeWidth = 1f
        )

        // График
        drawPath(
            path = path,
            color = Color(0xFF1B588C),
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawTextWithCanvas(
    text: String,
    color: Color,
    topLeft: Offset
) {

}

private fun DrawScope.drawGrid(
    padding: Float,
    width: Float,
    height: Float
) {
    val gridColor = Color.Gray.copy(alpha = 0.1f)

    // Вертикальные линии
    val verticalSteps = 10
    for (i in 0..verticalSteps) {
        val x = padding + (width / verticalSteps) * i
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, padding + height),
            strokeWidth = 1f
        )
    }

    // Горизонтальные линии
    val horizontalSteps = 8
    for (i in 0..horizontalSteps) {
        val y = padding + (height / horizontalSteps) * i
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + width, y),
            strokeWidth = 1f
        )
    }
}

// Простая функция для рисования текста (в реальном проекте используйте Text или другой метод)
private fun DrawScope.drawText(
    text: String,
    color: Color,
    topLeft: Offset
) {
    // Для простоты используем drawContext
    // В реальном приложении используйте NativeCanvas или другой подход
    // Здесь просто для демонстрации
}






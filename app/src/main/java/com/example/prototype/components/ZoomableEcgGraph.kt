package com.example.prototype.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.max
import kotlin.math.min

@Composable
fun ZoomableEcgGraph(
    ecgValues: List<Int>,
    timestamps: List<Long>,          // пока не используется, но можно добавить подписи позже
    modifier: Modifier = Modifier
) {
    if (ecgValues.isEmpty() || timestamps.isEmpty()) return

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }

    val pointSpacing = 10f // расстояние между точками при scale = 1 (в пикселях)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Изменяем масштаб
                        scale = (scale * zoom).coerceIn(1f, 10f)

                        // Корректируем смещение: при свайпе вправо (pan.x > 0) сдвигаемся вправо
                        offsetX -= pan.x

                        // Ограничиваем смещение, чтобы не уйти за пределы
                        val totalWidth = ecgValues.size * pointSpacing * scale
                        val maxOffsetX = max(0f, totalWidth - size.width)
                        offsetX = offsetX.coerceIn(0f, maxOffsetX)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            if (canvasWidth <= 0 || canvasHeight <= 0) return@Canvas

            // Вычисляем видимый диапазон индексов
            val startIndex = max(0, (offsetX / (pointSpacing * scale)).toInt())
            val endIndex = min(ecgValues.lastIndex, ((offsetX + canvasWidth) / (pointSpacing * scale)).toInt())
            if (startIndex > endIndex) return@Canvas

            // Определяем мин/макс среди видимых точек для вертикального масштабирования
            var minY = Int.MAX_VALUE
            var maxY = Int.MIN_VALUE
            for (i in startIndex..endIndex) {
                val v = ecgValues[i]
                if (v < minY) minY = v
                if (v > maxY) maxY = v
            }
            val rangeY = (maxY - minY).coerceAtLeast(1).toFloat()

            // Рисуем сетку (как в EcgGraph, но с шагом 50 пикселей)
            val gridColor = Color.Gray.copy(alpha = 0.3f)
            for (x in 0..canvasWidth.toInt() step 50) {
                drawLine(
                    color = gridColor,
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), canvasHeight),
                    strokeWidth = 1f
                )
            }
            for (y in 0..canvasHeight.toInt() step 50) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y.toFloat()),
                    end = Offset(canvasWidth, y.toFloat()),
                    strokeWidth = 1f
                )
            }

            // Рисуем линию ЭКГ
            val path = Path()
            for (i in startIndex..endIndex) {
                val x = i * pointSpacing * scale - offsetX
                val y = canvasHeight - ((ecgValues[i] - minY) / rangeY * canvasHeight)
                if (i == startIndex) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFF1B588C),
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Опционально: можно нарисовать центральную линию (как в EcgGraph)
            drawLine(
                start = Offset(0f, canvasHeight / 2),
                end = Offset(canvasWidth, canvasHeight / 2),
                color = Color.Gray.copy(alpha = 0.2f),
                strokeWidth = 1f
            )
        }
    }
}
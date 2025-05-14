package org.spb.project.model

import androidx.compose.ui.geometry.Offset

/**
 * UI-модель вершины:
 * offset  — центр окружности,
 * radius  — радиус,
 * color   — цвет
 */
data class CircleNode(
    var offset: Offset,
    val radius: Float = 20f,
    var color: Int = 0xFF0000FF.toInt()  // по умолчанию синий
)
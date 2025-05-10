package org.spb.project.model

import androidx.compose.ui.geometry.Offset

// Описывает узел графа в виде окружности с положением и радиусом
// offset — координаты центра окружности в пространстве канвы
// radius — радиус окружности (по умолчанию 20f)
data class CircleNode(
    var offset: Offset,
    val radius: Float = 20f
)
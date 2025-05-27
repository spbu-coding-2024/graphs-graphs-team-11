package org.spb.project.model

import androidx.compose.ui.geometry.Offset

/**
 * UI-модель узла графа для отображения на Canvas.
 *
 * @property offset координаты центра круга (узла) на холсте
 * @property radius радиус круга в пикселях (по умолчанию 20f)
 * @property color  цвет узла в формате ARGB (Int), по умолчанию — синий
 */
data class CircleNode(
    var offset: Offset,           // где рисовать центр узла
    val radius: Float = 20f,      // размер узла, одинаков для всех
    var color: Int = 0xFF0000FF.toInt()  // цвет в формате 0xAARRGGBB, здесь — синий
)
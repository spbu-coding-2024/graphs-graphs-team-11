package common

/**
 * Описание вершины графа.
 *
 * @param x      координата X
 * @param y      координата Y
 * @param color  цвет вершины (Int ARGB), по умолчанию черный
 */
open class Vertex(
    var x: Double,
    var y: Double,
    var color: Int = 0xFF000000.toInt()
)

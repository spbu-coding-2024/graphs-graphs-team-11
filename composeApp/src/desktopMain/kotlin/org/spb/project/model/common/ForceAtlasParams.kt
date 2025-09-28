package org.spb.project.model.common

/**
 * Параметры алгоритма ForceAtlas, контролирующие поведения узлов в графе.
 *
 * @property repulsion коэффициент силы отталкивания между узлами. Чем выше значение,
 * тем сильнее узлы будут разлетаться друг от друга.
 * @property attraction коэффициент силы притяжения узлов. Чем больше, тем сильнее
 * узлы стремятся сблизиться.
 * @property damping коэффициент демпфирования, который уменьшает скорость перемещения узлов
 * для стабилизации положения.
 * @property gravity сила гравитации, удерживающая узлы ближе к центру пространства
 * и предотвращающая чрезмерное разлетание.
 * @property maxDisplacement максимальное расстояние, на которое узел может сдвинуться
 * за одну итерацию алгоритма. Позволяет ограничить резкие скачки в позиционировании.
 */
data class ForceAtlasParams(
    val repulsion: Double,
    val attraction: Double,
    val damping: Double,
    val gravity: Double,
    val maxDisplacement: Double
)
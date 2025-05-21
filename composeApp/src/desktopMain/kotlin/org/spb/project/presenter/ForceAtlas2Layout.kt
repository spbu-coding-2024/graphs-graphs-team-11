package org.spb.project.presenter

import org.spb.project.common.Graph
import org.spb.project.common.Vertex
import org.spb.project.common.Edge
import kotlin.math.sqrt

/**
 * Реализация одного шага алгоритма ForceAtlas2 для расстановки вершин графа.
 *
 * Параметры:
 * @param repulsionConstant  сила отталкивания (Coulomb)
 * @param attractionConstant сила притяжения по рёбрам (Hooke)
 * @param damping            коэффициент демпфирования (уменьшает «шаг»)
 * @param gravity            сила «центростремительного» гравитационного притяжения к центру
 * @param maxDisplacement    максимальное смещение вершины за 1 итерацию
 */
class ForceAtlas2Layout(
    private val repulsionConstant: Double = 100.0,
    private val attractionConstant: Double = 0.1,
    private val damping: Double = 0.9,
    private val gravity: Double = 1.0,
    private val maxDisplacement: Double = 10.0
) {
    /**
     * Выполнить один итерационный шаг ForceAtlas2:
     *  1) Рассчитать отталкивающие силы между всеми парами вершин.
     *  2) Рассчитать притягивающие силы вдоль рёбер.
     *  3) Добавить «гравитацию», чтобы узлы не «улетали» слишком далеко.
     *  4) Ограничить максимальное смещение.
     *  5) Обновить позиции вершин.
     *
     * @param graph Граф, содержащий вершины с полями x, y и список рёбер.
     */
    fun applyLayout(graph: Graph) {
        // 1) Получаем вершины и список рёбер (каждое ребро хранит .vertex — индекс смежной вершины)
        val vertices: List<Vertex> = graph.getVertexes()
        val edges: List<List<Edge>> = graph.getEdges()   // теперь — List<List<Edge>>, не List<List<Int>>
        val n = vertices.size

        // 2) Массивы для накопления векторов силы по осям X и Y
        val forceX = DoubleArray(n) { 0.0 }
        val forceY = DoubleArray(n) { 0.0 }

        // 3) Отталкивающие силы (Coulomb) между всеми парами i<j
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val vi = vertices[i]
                val vj = vertices[j]
                val dx = vi.x - vj.x
                val dy = vi.y - vj.y
                // Добавляем эпсилон, чтобы исключить деление на ноль
                val dist2 = dx * dx + dy * dy + 0.01
                val dist  = sqrt(dist2)
                // Сила Кулона: K_r / r^2
                val f = repulsionConstant / dist2
                // Вектор единичной длины * сила
                val fx = (dx / dist) * f
                val fy = (dy / dist) * f

                // Прикладываем к двум вершинам в противоположных направлениях
                forceX[i] += fx; forceY[i] += fy
                forceX[j] -= fx; forceY[j] -= fy
            }
        }

        // 4) Притягивающие силы (Hooke) вдоль всех рёбер
        edges.forEachIndexed { i, adjList ->
            val vi = vertices[i]
            adjList.forEach { edge ->
                val j = edge.vertex
                val vj = vertices[j]
                val dx = vj.x - vi.x
                val dy = vj.y - vi.y
                // Длина + эпсилон
                val dist = sqrt(dx * dx + dy * dy) + 0.01
                // Сила Гука: K_a * r
                val f = attractionConstant * dist
                val fx = (dx / dist) * f
                val fy = (dy / dist) * f

                forceX[i] += fx; forceY[i] += fy
                forceX[j] -= fx; forceY[j] -= fy
            }
        }

        // 5) «Гравитация» к центру координат (0,0)
        for (i in 0 until n) {
            val v = vertices[i]
            val dx = -v.x
            val dy = -v.y
            val dist = sqrt(dx * dx + dy * dy) + 0.01
            forceX[i] += gravity * dx / dist
            forceY[i] += gravity * dy / dist
        }

        // 6) Обновление позиций: демпфирование + ограничение maxDisplacement
        for (i in 0 until n) {
            // Изначальное смещение
            var dx = forceX[i] * damping
            var dy = forceY[i] * damping

            // Ограничиваем длину вектора смещения
            val disp = sqrt(dx * dx + dy * dy)
            if (disp > maxDisplacement) {
                dx = dx / disp * maxDisplacement
                dy = dy / disp * maxDisplacement
            }

            // Применяем к координатам
            vertices[i].x += dx
            vertices[i].y += dy
        }
    }
}

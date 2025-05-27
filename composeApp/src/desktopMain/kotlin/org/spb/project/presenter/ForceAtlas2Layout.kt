package org.spb.project.presenter

import org.spb.project.common.Graph
import org.spb.project.common.Vertex
import org.spb.project.common.Edge
import kotlin.math.sqrt

/**
 * Пошаговая реализация алгоритма ForceAtlas2 для автоматической расстановки узлов графа.
 * Каждый вызов applyLayout смещает вершины под действием:
 *  - отталкивания между всеми вершинами (Кулон)
 *  - притяжения вдоль рёбер (закон Гука)
 *  - "гравитации" к центру координат
 *  - демпфирования и ограничения максимального смещения
 *
 * @param repulsionConstant  сила отталкивания (чем больше — тем сильнее узлы разлетаются)
 * @param attractionConstant сила притяжения по рёбрам (чем больше — тем сильнее узлы тянутся друг к другу)
 * @param damping            коэффициент демпфирования, сглаживает движение
 * @param gravity            сила притяжения к точке (0,0), чтобы узлы не улетали слишком далеко
 * @param maxDisplacement    максимальное смещение вершины за одну итерацию
 */
class ForceAtlas2Layout(
    private val repulsionConstant: Double = 100.0,
    private val attractionConstant: Double = 0.1,
    private val damping: Double = 0.9,
    private val gravity: Double = 1.0,
    private val maxDisplacement: Double = 10.0
) {
    /**
     * Выполнить один шаг алгоритма ForceAtlas2 на переданном графе:
     * 1. Рассчитать отталкивающие силы между всеми парами вершин.
     * 2. Рассчитать притягивающие силы вдоль рёбер.
     * 3. Применить гравитацию к центру.
     * 4. Ограничить макс. смещение и применить демпфирование.
     * 5. Обновить позиции вершин.
     *
     * @param graph источник вершин и рёбер
     */
    fun applyLayout(graph: Graph) {
        // Получаем текущие вершины и списки их рёбер
        val vertices: List<Vertex> = graph.getVertexes()
        val edges: List<List<Edge>> = graph.getEdges()
        val n = vertices.size

        // Массивы для накопления векторов силы по X и Y
        val forceX = DoubleArray(n) { 0.0 }
        val forceY = DoubleArray(n) { 0.0 }

        // 1) Кулоновское отталкивание между всеми парами i < j
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val vi = vertices[i]
                val vj = vertices[j]
                val dx = vi.x - vj.x
                val dy = vi.y - vj.y
                // небольшой эпсилон, чтобы не было деления на ноль
                val dist2 = dx * dx + dy * dy + 0.01
                val dist = sqrt(dist2)
                // сила отталкивания: repulsionConstant / r^2
                val f = repulsionConstant / dist2
                // компоненты вектора силы
                val fx = (dx / dist) * f
                val fy = (dy / dist) * f

                // прикладываем к двум вершинам в разные стороны
                forceX[i] += fx; forceY[i] += fy
                forceX[j] -= fx; forceY[j] -= fy
            }
        }

        // 2) Притяжение по рёбрам (закон Гука)
        edges.forEachIndexed { i, adjList ->
            val vi = vertices[i]
            adjList.forEach { edge ->
                val j = edge.vertex
                val vj = vertices[j]
                val dx = vj.x - vi.x
                val dy = vj.y - vi.y
                // длина ребра + эпсилон
                val dist = sqrt(dx * dx + dy * dy) + 0.01
                // сила упругости: attractionConstant * r
                val f = attractionConstant * dist
                val fx = (dx / dist) * f
                val fy = (dy / dist) * f

                forceX[i] += fx; forceY[i] += fy
                forceX[j] -= fx; forceY[j] -= fy
            }
        }

        // 3) Гравитация к центру координат (0,0)
        for (i in 0 until n) {
            val v = vertices[i]
            val dx = -v.x  // вектор к центру
            val dy = -v.y
            val dist = sqrt(dx * dx + dy * dy) + 0.01
            forceX[i] += gravity * dx / dist
            forceY[i] += gravity * dy / dist
        }

        // 4) Ограничение смещения и демпфирование перед обновлением
        for (i in 0 until n) {
            var dx = forceX[i] * damping
            var dy = forceY[i] * damping

            val disp = sqrt(dx * dx + dy * dy)
            if (disp > maxDisplacement) {
                dx = dx / disp * maxDisplacement
                dy = dy / disp * maxDisplacement
            }

            // 5) Обновляем координаты вершины
            vertices[i].x += dx
            vertices[i].y += dy
        }
    }
}
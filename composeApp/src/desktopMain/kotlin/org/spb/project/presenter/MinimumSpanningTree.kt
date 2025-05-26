package org.spb.project.presenter

import org.spb.project.common.Graph

/**
 * Построение минимального остовного дерева (МИНИМАЛЬНОГО ОСТОВА) в неориентированном
 * взвешенном графе по алгоритму Крускала.
 *
 * Сложность: O(E log E) из-за сортировки списка рёбер, где E — число рёбер.
 */
class MinimumSpanningTree {

    /**
     * Представление ребра остова.
     * @param u     индекс одной вершины
     * @param v     индекс другой вершины
     * @param weight вес ребра
     */
    data class MSTEdge(val u: Int, val v: Int, val weight: Int)

    /**
     * Построить список рёбер минимального остовного дерева.
     *
     * @param graph Неориентированный взвешенный граф. Метод getEdges() должен возвращать
     *              для каждой вершины i список объектов Edge, где Edge.vertex — индекс
     *              смежной вершины, а Edge.weight — вес ребра.
     * @return Список ребер MST, отсортированный в порядке включения (не обязательно по весу).
     */
    fun buildMST(graph: Graph): List<MSTEdge> {
        // 1) Число вершин
        val n = graph.getVertexes().size

        // 2) Собираем все уникальные ребра (u < v), чтобы не дублировать каждое дважды
        val allEdges = mutableListOf<MSTEdge>()
        graph.getEdges().forEachIndexed { u, adjList ->
            for (edge in adjList) {
                val v = edge.vertex
                val w = edge.weight  // предполагается, что Edge хранит вес
                if (u < v) {
                    allEdges.add(MSTEdge(u, v, w))
                }
            }
        }

        // 3) Сортируем список ребер по возрастанию веса
        allEdges.sortBy { it.weight }

        // 4) Инициализируем структуру непересекающихся множеств (DSU) для n вершин
        val dsu = DisjointSet(n)

        // 5) Алгоритм Крускала: перебираем ребра по ранжиру, добавляем, если не создаёт цикл
        val mst = mutableListOf<MSTEdge>()
        for (e in allEdges) {
            if (dsu.find(e.u) != dsu.find(e.v)) {
                dsu.union(e.u, e.v)
                mst.add(e)
            }
        }

        // 6) Возвращаем список ребер минимального остова
        return mst
    }

    /**
     * Внутренний класс для структуры непересекающихся множеств (Union-Find) с путём сжатия
     * и ранговой эвристикой.
     */
    private class DisjointSet(n: Int) {
        private val parent = IntArray(n) { it }
        private val rank   = IntArray(n) { 0 }

        /** Найти корневого представителя множества, применяя сжатие пути. */
        fun find(x: Int): Int {
            if (parent[x] != x) {
                parent[x] = find(parent[x])
            }
            return parent[x]
        }

        /** Объединить множества, в которые входят x и y, по ранговой эвристике. */
        fun union(x: Int, y: Int) {
            val rx = find(x)
            val ry = find(y)
            if (rx == ry) return  // уже в одном множестве

            // Присоединяем более «низкий» ранг к более «высокому»
            when {
                rank[rx] < rank[ry] -> parent[rx] = ry
                rank[ry] < rank[rx] -> parent[ry] = rx
                else -> {
                    parent[ry] = rx
                    rank[rx]++
                }
            }
        }
    }
}

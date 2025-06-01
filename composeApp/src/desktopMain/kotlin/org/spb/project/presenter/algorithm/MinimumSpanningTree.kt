package org.spb.project.presenter.algorithm

import org.spb.project.model.common.Graph

/**
 * Алгоритм Крускала для построения минимального остовного дерева (MST)
 * в неориентированном взвешенном графе.
 * Временная сложность: O(E log E) за счёт сортировки ребёр,
 * где E — число ребёр.
 */
class MinimumSpanningTree {

    /**
     * Представление ребра в MST.
     * @param u      одна из вершин ребра
     * @param v      вторая вершина ребра
     * @param weight вес ребра
     */
    data class MSTEdge(
        val u: Int,
        val v: Int,
        val weight: Int
    )

    /**
     * Строит список ребер минимального остова для заданного графа.
     * 1. Собираем все уникальные ребра (u < v).
     * 2. Сортируем их по возрастанию веса.
     * 3. С помощью структуры непересекающихся множеств (DSU)
     *    добавляем ребра, не образующие циклов.
     *
     * @param graph ориентированный или неориентированный взвешенный граф
     * @return список ребер MST в порядке добавления
     */
    fun buildMST(graph: Graph): List<MSTEdge> {
        val n = graph.getVertexes().size  // количество вершин

        // Составляем список всех уникальных ребер (u < v) — чтобы не дублировать
        val allEdges = mutableListOf<MSTEdge>()
        graph.getEdges().forEachIndexed { u, adjList ->
            adjList.forEach { edge ->
                val v = edge.vertex
                if (u < v) {
                    allEdges.add(MSTEdge(u, v, edge.weight))
                }
            }
        }

        // Сортируем по весу, от лёгких к тяжёлым
        allEdges.sortBy { it.weight }

        // Инициализируем DSU для проверки циклов
        val dsu = DisjointSet(n)
        val mst = mutableListOf<MSTEdge>()

        // Основной цикл Крускала: добавляем ребро, если оно соединяет разные компоненты
        for (e in allEdges) {
            if (dsu.find(e.u) != dsu.find(e.v)) {
                dsu.union(e.u, e.v)
                mst.add(e)
            }
        }

        return mst
    }

    /**
     * Внутренняя реализация структуры непересекающихся множеств (Union-Find)
     * с путевым сжатием и ранговой эвристикой.
     */
    private class DisjointSet(size: Int) {
        private val parent = IntArray(size) { it }  // изначально каждый элемент в своём множестве
        private val rank = IntArray(size) { 0 }     // ранговая эвристика

        /**
         * Находит корень x и одновременно сжимает путь для оптимизации.
         */
        fun find(x: Int): Int {
            if (parent[x] != x) {
                parent[x] = find(parent[x])  // сжимаем путь
            }
            return parent[x]
        }

        /**
         * Объединяет множества x и y по рангу,
         * чтобы дерево оставалось максимально плоским.
         */
        fun union(x: Int, y: Int) {
            val rootX = find(x)
            val rootY = find(y)
            if (rootX == rootY) return    // уже в одном множестве

            // Присоединяем менее «тяжёлое» дерево к более «тяжёлому»
            when {
                rank[rootX] < rank[rootY] -> parent[rootX] = rootY
                rank[rootX] > rank[rootY] -> parent[rootY] = rootX
                else -> {
                    parent[rootY] = rootX
                    rank[rootX]++
                }
            }
        }
    }
}
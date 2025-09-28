package org.spb.project.presenter.algorithm

import org.spb.project.model.common.Graph
import org.spb.project.model.common.Edge

/**
 * Алгоритм Косараджу для поиска сильносвязных компонент в ориентированном графе.
 * Использует два обхода в глубину (DFS):
 *  1. Первый обход формирует порядок вершин по убыванию времени выхода.
 *  2. Второй обход по обратному графу собирает все вершины в компоненты.
 */
class StronglyConnectedComponents {
    /**
     * Ищет все сильносвязные компоненты графа и возвращает список списков вершин.
     * @param graph ориентированный граф
     * @return список компонент, каждая — список индексов вершин
     */
    fun findComponents(graph: Graph): List<List<Int>> {
        val n = graph.getVertexes().size            // общее число вершин
        val edges: List<List<Edge>> = graph.getEdges() // смежные списки исходящего графа

        // 1) Первый DFS: заполняем список order по убыванию времени выхода
        val visited = BooleanArray(n)               // флаги посещения
        val order = mutableListOf<Int>()            // порядок выхода из рекурсии

        // рекурсивный обход: отмечаем вершину, обходим соседей, затем добавляем в order
        fun dfs1(u: Int) {
            visited[u] = true
            for (e in edges[u]) {
                val v = e.vertex
                if (!visited[v]) dfs1(v)
            }
            order.add(u)  // после всех потомков добавляем текущую вершину
        }

        // запускаем первый DFS для всех непройденных вершин
        for (u in 0 until n) {
            if (!visited[u]) dfs1(u)
        }

        // 2) Строим обратный граф: ребро u->v преобразуем в v->u
        val rev = List(n) { mutableListOf<Int>() }
        for (u in 0 until n) {
            for (e in edges[u]) {
                rev[e.vertex].add(u)
            }
        }

        // готовим массив посещения для второго DFS и результат
        visited.fill(false)
        val components = mutableListOf<List<Int>>()  // здесь будем собирать компоненты

        // второй DFS: обходит обратный граф, начиная с вершины u
        fun dfs2(u: Int, comp: MutableList<Int>) {
            visited[u] = true
            comp.add(u)  // включаем вершину в текущую компоненту
            for (v in rev[u]) {
                if (!visited[v]) dfs2(v, comp)
            }
        }

        // запускаем второй DFS в порядке убывания order
        for (u in order.asReversed()) {
            if (!visited[u]) {
                val comp = mutableListOf<Int>()
                dfs2(u, comp)
                components.add(comp)  // сохраняем заполненную компоненту
            }
        }

        return components
    }
}
package org.spb.project.presenter

import org.spb.project.common.Graph

/**
 * Алгоритм поиска сильносвязных компонент (Kosaraju) для ориентированного графа.
 */
class StronglyConnectedComponents {
    fun findComponents(graph: Graph): List<List<Int>> {
        val n = graph.getVertexes().size
        val edges = graph.getEdges()

        val visited = BooleanArray(n)
        val order = mutableListOf<Int>()

        fun dfs1(u: Int) {
            visited[u] = true
            edges[u].forEach { e -> if (!visited[e.vertex]) dfs1(e.vertex) }
            order += u
        }
        for (u in 0 until n) if (!visited[u]) dfs1(u)

        // строим обратный граф
        val rev = List(n) { mutableListOf<Int>() }
        edges.forEachIndexed { u, list -> list.forEach { e -> rev[e.vertex].add(u) } }

        visited.fill(false)
        val components = mutableListOf<List<Int>>()

        fun dfs2(u: Int, comp: MutableList<Int>) {
            visited[u] = true; comp += u
            rev[u].forEach { v -> if (!visited[v]) dfs2(v, comp) }
        }
        for (u in order.asReversed()) {
            if (!visited[u]) {
                val comp = mutableListOf<Int>()
                dfs2(u, comp)
                components += comp
            }
        }
        return components
    }
}
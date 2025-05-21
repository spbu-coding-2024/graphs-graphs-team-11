package org.spb.project.presenter

import org.spb.project.common.Graph
import org.spb.project.common.Edge

/**
 * Алгоритм Косараджу для поиска сильносвязных компонент в ориентированном графе.
 * Реализован двумя проходами DFS: первый - на оригинальном графе, второй - на обратном.
 */
class StronglyConnectedComponents {
    /**
     * Находит все сильносвязные компоненты графа и возвращает их в виде списка списков вершин.
     * @param graph объект графа, реализующий интерфейс Graph
     * @return список сильносвязных компонент, каждая из которых представлена списком индексов вершин
     */
    fun findComponents(graph: Graph): List<List<Int>> {
        // Число вершин в графе
        val n = graph.getVertexes().size
        // Список смежности оригинального графа: для каждой вершины - список ребёр
        val edges: List<List<Edge>> = graph.getEdges()

        // Массив для отметки посещённых вершин
        val visited = BooleanArray(n)
        // Список вершин в порядке невозрастающего времени выхода из dfs1
        val order = mutableListOf<Int>()

        /**
         * Первый DFS: обходит граф в глубину и заполняет список order вершинами по 'выходу'.
         */
        fun dfs1(u: Int) {
            visited[u] = true  // отмечаем вершину как посещённую
            // рекурсивно обходим все соседние вершины
            for (e in edges[u]) {
                val v = e.vertex
                if (!visited[v]) dfs1(v)
            }
            // добавляем вершину в order после обработки всех её смежных вершин
            order.add(u)
        }

        // Запуск первого DFS для всех непосещённых вершин
        for (u in 0 until n) {
            if (!visited[u]) dfs1(u)
        }

        // Построение обратного графа: для каждого ребра u->v добавляем в rev[v] вершину u
        val rev = List(n) { mutableListOf<Int>() }
        for (u in 0 until n) {
            for (e in edges[u]) {
                rev[e.vertex].add(u)
            }
        }

        // Сброс массива visited для второго прохода
        visited.fill(false)
        // Список для хранения найденных компонент
        val components = mutableListOf<List<Int>>()

        /**
         * Второй DFS: обходит обратный граф и собирает вершины в одну компоненту.
         */
        fun dfs2(u: Int, comp: MutableList<Int>) {
            visited[u] = true       // отмечаем вершину
            comp.add(u)              // добавляем в текущую компоненту
            // продолжаем обход по обратному графу
            for (v in rev[u]) {
                if (!visited[v]) dfs2(v, comp)
            }
        }

        // Обход вершины в порядке убывающего времени выхода из первого DFS
        for (u in order.asReversed()) {
            if (!visited[u]) {
                val comp = mutableListOf<Int>()
                dfs2(u, comp)           // собираем компоненты
                components.add(comp)     // сохраняем компоненту
            }
        }

        // Возвращаем список всех сильносвязных компонент
        return components
    }
}
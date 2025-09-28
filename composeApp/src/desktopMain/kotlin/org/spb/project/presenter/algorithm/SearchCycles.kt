package org.spb.project.presenter.algorithm

import org.spb.project.model.common.Graph
import kotlin.collections.mutableListOf

/**
 * Реализация алгоритма DLPA поиска циклов, для выбранной вершины
 *
 * Параметры:
 * @param graph граф, в котором выполняем алгоритм
 * @param start вершина, у которой ищем циклы
 */
class SearchCycles(graph: Graph, start: Int) {


    private val edges = graph.getEdges()
    private var visited = mutableSetOf<Int>()
    private var path = mutableListOf<Int>()

    /**
     * Список, в котором храняться найденные циклы
     */
    var cycles = mutableListOf<MutableList<Int>>()


    var startVertex = start

    fun dfs(v: Int) {

        if (v == startVertex) {
            if (path.isNotEmpty()) {
                cycles.add(mutableListOf())
                for (elem in path) {
                    cycles.last().add(elem)
                }
                cycles.last().add(startVertex)
                return
            }
        }

        if (v in visited) {
            return
        }

        visited.add(v)
        path.add(v)


        for (neighbor in edges[v]) {
            dfs(neighbor.vertex)
        }

        path.removeLast()
        visited.remove(v)
    }

    /**
     * Функция выполняет поиск циклов, вызывая dfs
     * @return Список циклов
     */
    fun search(): List<List<Int>> {
        for (neighbor in edges[startVertex]) {
            visited = mutableSetOf<Int>()
            path = mutableListOf<Int>()
            dfs(neighbor.vertex)
        }
        return cycles
    }
}
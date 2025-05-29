package org.spb.project.presenter

import org.spb.project.common.*

/**
 * Реализация алгоритма Форд-Белмана для поиска минимальных расстояний до вершин
 * и кратчайшего пути для выбранной вершины
 *
 * Параметры:
 * @param graph граф, в котором выполняем поиск пути
 * @param start вершина, от который ищем расстояния
 * @param finish вершина в кторую ищем кратчайщий путь
 */
class FordBelmanShortPath(graph: Graph, start: Int, finish: Int){
    private val edges = graph.getEdges()
    private val n = graph.getVertexes().size

    /**
     * В качестве большого числа, для максимального расстояния взято 2147483647
     */
    private var inf = 2147483647
    var startVertex = start
    var finishVertex = finish

    /**
     * Список с расстояними до всех вершин
     */
    var distances = mutableListOf<Int>()


    var predessors = mutableListOf<Int>()

    /**
     * Реализация алгоритма Форд-Белмана
     */
    fun fordBelman(): Int{
        while (distances.size < n){
            distances.add(inf)
            predessors.add(-1)
        }
        distances[startVertex] = 0
        for (i in 0..n-2){
            var j = 0
            while (j < edges.size){
                var e = 0
                while (e < edges[j].size){
                    if (distances[j] != inf){
                        if (distances[j] + edges[j][e].weight < distances[edges[j][e].vertex]){
                            distances[edges[j][e].vertex] = distances[j] + edges[j][e].weight
                            predessors[edges[j][e].vertex] = j
                        }
                    }
                    e = e + 1
                }
                j = j + 1
            }
        }
        var j = 0
        while (j < edges.size){
            var e = 0
            while (e < edges[j].size){
                if (distances[j] != inf){
                    if (distances[j] + edges[j][e].weight < distances[edges[j][e].vertex]){
                        return -inf
                    }
                }
                e = e + 1
            }
            j = j + 1
        }
        return distances[finishVertex]
    }

    /**
     * Функция вызывает алгоритм Форд-Белмана и восстанавливает путь из начальной вершины к конечной
     */
    fun getShortestPath(): List<Int>?{
        if( fordBelman() == -inf ){
            return mutableListOf<Int>()
        }
        val path = mutableListOf<Int>()
        var current = finishVertex
        while (current != startVertex){
            if (current == -1){
                return mutableListOf<Int>()
            }
            path.addFirst(current)
            current = predessors[current]
        }
        path.addFirst(startVertex)
        return path
    }
}
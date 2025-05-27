package org.spb.project.common

/**
 * Класс Graph хранит список вершин и рёбер, а также знает свой тип:
 * обычный (NON_ORIENTED), ориентированный или взвешенный.
 *
 * @property typeOfGraph определяет, как работать с рёбрами (ориентированный/взвешенный и т.п.)
 */
class Graph(private var typeOfGraph: GraphType) {

    // Список вершин: каждая вершина знает свою позицию и цвет
    private val vertexes = mutableListOf<Vertex>()

    // Список смежных списков рёбер: для каждой вершины — свой список исходящих рёбер
    private val edges = mutableListOf<MutableList<Edge>>()

    /**
     * Возвращает текущий тип графа (NON_ORIENTED, ORIENTED, WEIGHTED и т.д.).
     */
    fun getType(): GraphType = typeOfGraph

    /**
     * Добавляет новую вершину с координатами (x, y) и цветом.
     * Рядом создаётся пустой список рёбер для этой вершины.
     *
     * @param x      положение по горизонтали
     * @param y      положение по вертикали
     * @param color  ARGB-цвет (по умолчанию — чёрный)
     */
    fun addVertex(x: Double, y: Double, color: Int = 0xFF000000.toInt()) {
        vertexes.add(Vertex(x, y, color))
        // У каждой вершины должен быть свой список исходящих рёбер
        edges.add(mutableListOf())
    }

    /**
     * Добавляет ребро из вершины startVertex в вершину finalVertex.
     * Если списка рёбер для startVertex ещё нет, расширяем контейнер.
     *
     * @param startVertex   индекс исходной вершины
     * @param finalVertex   индекс целевой вершины
     * @param weight        вес ребра (например, расстояние или стоимость)
     * @param color         ARGB-цвет для визуализации ребра
     */
    fun addEdge(startVertex: Int, finalVertex: Int, weight: Int, color: Int) {
        // Если startVertex лежит за текущим концом списка, добавляем промежуточные пустые списки
        if (edges.size < startVertex + 1) {
            while (edges.size < startVertex + 1) {
                edges.add(mutableListOf())
            }
        }
        // Добавляем ребро в список исходящих от startVertex
        edges[startVertex].add(Edge(finalVertex, weight, color))
    }

    /**
     * Позволяет получить изменяемый список вершин.
     * Используется, чтобы можно было напрямую удалять, очищать или добавлять вершины.
     *
     * @return MutableList<Vertex> — рабочая коллекция вершин
     */
    fun getVertexes(): MutableList<Vertex> = vertexes

    /**
     * Позволяет получить изменяемый список списков рёбер.
     * Это даёт полный контроль над матрицей смежности: добавление/удаление рёбер вручную.
     *
     * @return MutableList<MutableList<Edge>> — смежный список рёбер
     */
    fun getEdges(): MutableList<MutableList<Edge>> = edges
}
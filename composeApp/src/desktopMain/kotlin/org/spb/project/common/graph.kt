package common

/**
 * Граф с заранее заданным типом (NORMAL, ORIENTED, WEIGHTED)
 */
open class Graph(type: GraphType) {
    private var typeOfGraph = type
    fun getType(): GraphType = typeOfGraph

    private val vertexes = mutableListOf<Vertex>()
    private val edges = mutableListOf<MutableList<Edge>>()

    fun changeType(t: GraphType) {
        typeOfGraph = t
    }

    /**
     * Добавить вершину с цветом
     * @param x координата X
     * @param y координата Y
     * @param color ARGB-цвет вершины
     */
    fun addVertex(x: Double, y: Double, color: Int = 0xFF0000FF.toInt()) {
        vertexes.add(Vertex(x, y, color))
    }

    /**
     * Добавить ребро с цветом
     */
    fun addEdge(startVertex: Int, finalVertex: Int, weight: Int, color: Int) {
        if (edges.size < startVertex + 1) {
            while (edges.size < startVertex + 1) {
                edges.add(mutableListOf())
            }
        }
        edges[startVertex].add(Edge(finalVertex, weight, color))
    }

    fun getVertexes(): List<Vertex> = vertexes
    fun getEdges(): List<List<Edge>>   = edges
}

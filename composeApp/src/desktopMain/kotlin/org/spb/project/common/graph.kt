package common

/**
 * Граф с заранее заданным типом (NORMAL, ORIENTED, WEIGHTED)
 */
class Graph(private var typeOfGraph: GraphType) {

    private val vertexes = mutableListOf<Vertex>()
    private val edges    = mutableListOf<MutableList<Edge>>()

    fun getType(): GraphType = typeOfGraph
    fun changeType(t: GraphType) { typeOfGraph = t }

    /**
     * Добавить вершину.
     * @param x координата X
     * @param y координата Y
     * @param color ARGB-цвет в виде Int
     */
    fun addVertex(x: Double, y: Double, color: Int = 0xFF000000.toInt()) {
        vertexes.add(Vertex(x, y, color))
    }

    /**
     * Добавить ребро.
     * @param startVertex индекс начальной вершины
     * @param finalVertex индекс конечной вершины
     * @param weight вес ребра
     * @param color ARGB-цвет в виде Int
     */
    fun addEdge(startVertex: Int, finalVertex: Int, weight: Int, color: Int) {
        if (edges.size < startVertex + 1) {
            while (edges.size < startVertex + 1) {
                edges.add(mutableListOf())
            }
        }
        edges[startVertex].add(Edge(finalVertex, weight, color))
    }

    /**
     * Возвращает **изменяемый** список вершин,
     * чтобы можно было делать removeAt(), add(), clear() и т.п.
     */
    fun getVertexes(): MutableList<Vertex> = vertexes

    /**
     * Возвращает **изменяемый** список списков рёбер,
     * чтобы можно было править матрицу смежности на лету.
     */
    fun getEdges(): MutableList<MutableList<Edge>> = edges
}
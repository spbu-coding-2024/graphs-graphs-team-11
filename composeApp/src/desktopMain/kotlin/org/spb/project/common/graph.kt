package common


open class Graph(type: GraphType) {
    private var typeOfGraph = type
    private var vertexes = mutableListOf<Vertex>()
    private var edges = mutableListOf<MutableList<Edge>>()

    /*
        Изменяем тип графа
        Типы графа: Обычный, Ориентированный, Взвешенный
    */
    fun changeType(t: GraphType) {
        typeOfGraph = t
    }

    /*
    Функция добавления ребра
    В качестве аргументов: Номер вершины от которой начинается ребро (нумерация с 0), Номер вершины куда идёт ребро
    Вес ребра(1 в невзвешенном графе) и цвет ребра (Пока что Int, потом поменяем)
    */
    fun addEdge(startVertex: Int, finalVertex: Int, weight: Int, color: Int){
        if (edges.size < startVertex + 1) {
            while (edges.size < startVertex + 1) {
                edges.add(mutableListOf<Edge>())
            }
        }
        edges[startVertex].add(Edge(finalVertex, weight, color))
    }

    /*
    Функция добавления вершины
    В качестве аргументов: координаты x, y
    */
    fun addVertex(x: Double, y: Double){
        vertexes.add(Vertex(x,y))
    }

    /*
    Возврат матрицы смежности
    Временно небезопасно
    */
    fun getEdges(): MutableList<MutableList<Edge>>{
        return edges
    }
    
    /*
    Возврат всего списка вершин
    Временно небезопасно
    */
    fun getVertexes(): MutableList<Vertex> {
        return vertexes
    }

}


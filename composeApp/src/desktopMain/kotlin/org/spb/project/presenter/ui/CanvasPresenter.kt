package org.spb.project.presenter.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.spb.project.model.common.*
import org.spb.project.model.ui.CircleNode
import org.spb.project.presenter.filereader.RWCSV
import org.spb.project.presenter.algorithm.*
import org.spb.project.presenter.database.GraphDbHelper
import org.spb.project.presenter.database.neo4jDb
import kotlin.random.Random
import kotlin.math.min

/**
 * Презентер, отвечающий за логику работы с графом и синхронизацию модели и UI-слоя.
 *
 * Основные обязанности:
 * 1. Хранит экземпляр графа и управляет его состоянием (загрузка, сохранение, модификации).
 * 2. Обеспечивает реализацию алгоритмов визуализации и анализа графа (ForceAtlas2, SCC, MST и др.).
 * 3. Управляет списком узлов (`CircleNode`) для отображения на Canvas и передаёт новые координаты в модель.
 * 4. Обрабатывает события пользовательского ввода: добавление/удаление/окрашивание узлов, перетаскивание, масштабирование и т.д.
 */
class CanvasPresenter {
    // Текущий граф (изначально — невзвешенный неориентированный)
    private var graph by mutableStateOf(Graph(GraphType.NON_ORIENTED))

    // Помощник для работы с SQLite-базой
    private val db = GraphDbHelper

    // Клиент для работы с Neo4j (Bolt-протокол)
    private val neo4jDb = neo4jDb("bolt://localhost:7687", "neo4j", "lolkekcheb")

    // Алгоритм ForceAtlas2 для раскладки вершин
    private var forceAtlas = ForceAtlas2Layout()

    // Поисковик сильно связных компонентов
    private val sccFinder = StronglyConnectedComponents()

    // Добавляем поле для MST-алгоритма
    private val mstBuilder = MinimumSpanningTree()

    /**
     * Текущий тип графа. Используется для управления доступностью некоторых алгоритмов.
     */
    val graphType: GraphType get() = graph.getType()

    // UI-список вершин
    private val nodesList = mutableStateListOf<CircleNode>()
    val circleNodes: List<CircleNode> get() = nodesList

    // Список рёбер из модели
    val edges: List<List<Edge>> get() = graph.getEdges()

    // Список вершин
    val vertexes: List<Vertex> get() = graph.getVertexes()

    // Текущий выбранный узел (или null)
    var selectedNodeIndex by mutableStateOf<Int?>(null)
        private set
    var memorizedVertex = 0

    // Индекс узла, который сейчас перетаскивается (или null, если перетаскивается канвас)
    private var activeDragIndex by mutableStateOf<Int?>(null)

    /**
     * Текущий масштаб (zoom). Коэффициент масштабирования канваса.
     * Принимает значения от 0.1 до 5. Начальное значение — 1f.
     */
    var zoom by mutableStateOf(1f); private set

    /**
     * Текущая панорамная смещение (pan). Используется при перетаскивании канваса.
     */
    var pan by mutableStateOf(Offset.Zero); private set
    var result by mutableStateOf("")
    var errorNeo4j by mutableStateOf(0)

    init {
        loadGraph()
    }

    /**
     * Устанавливает выделенный узел по переданному индексу.
     *
     * @param index индекс узла в списке vertices или null, чтобы снять выделение.
     */
    fun selectNode(index: Int?) {
        selectedNodeIndex = index
    }

    /**
     * Добавляет новую вершину в модель и в UI.
     *
     * Если есть выделенный узел, новая вершина появится в смещении (+50,+50)
     * относительно него и автоматически соединится с ним ребром весом 1 (цвет по умолчанию).
     * Если ничего не выделено, вершина появляется в центре (Offset(500f, 800f)).
     *
     * @param color цвет новой вершины (ARGB Int).
     */
    fun addCircle(color: Int) {
        // Вычисляем позицию: либо рядом с выделенным узлом, либо по умолчанию
        val pos = if (selectedNodeIndex != null) {
            val sel = nodesList[selectedNodeIndex!!]
            sel.offset + Offset(50f, 50f)
        } else {
            Offset(500f, 800f)
        }

        // 1) Модель: добавляем вершину
        graph.addVertex(pos.x.toDouble(), pos.y.toDouble(), color)
        val newIdx = graph.getVertexes().lastIndex

        // Если был выбран узел — создаём ребро между ним и новой вершиной
        selectedNodeIndex?.let { selIdx ->
            val edgeColor = 0xFF888888.toInt()
            graph.addEdge(selIdx, newIdx, 1, edgeColor)
        }

        // 2) UI: добавляем CircleNode
        nodesList.add(CircleNode(pos, color = color))
    }

    /**
     * Удаляет выделенную вершину и все инцидентные рёбра в модели и в UI.
     *
     * Последовательность действий:
     * 1. Удаляем вершину из списка vertexes (модель).
     * 2. Удаляем все исходящие из неё рёбра (list.removeAt).
     * 3. В оставшихся списках рёбер удаляем ребра, которые вели в неё,
     *    и «перенумеровываем» вершины с большим индексом (vertex > idx) — уменьшаем на 1.
     * 4. Удаляем соответствующий CircleNode из nodesList и снимаем выделение.
     * 5. Сбрасываем memorizedVertex в 0.
     */
    fun deleteSelectedNode() {
        selectedNodeIndex?.let { idx ->
            // --- 1) Модель: удаляем саму вершину ---
            if (idx in graph.getVertexes().indices) {
                graph.getVertexes().removeAt(idx)
            }
            // --- 2) Модель: удаляем список исходящих рёбер этой вершины ---
            if (idx in graph.getEdges().indices) {
                graph.getEdges().removeAt(idx)
            }
            // --- 3) Модель: корректируем оставшиеся списки рёбер ---
            graph.getEdges().forEach { list ->
                // удаляем все ребра, ведущие в idx
                list.removeAll { it.vertex == idx }
                // уменьшаем vertex у ребёр, индекс которых больше idx
                for (i in list.indices) {
                    val e = list[i]
                    if (e.vertex > idx) {
                        list[i] = Edge(
                            vertex = e.vertex - 1,
                            weight = e.weight,
                            color = e.color
                        )
                    }
                }
            }

            // --- 4) UI: удаляем узел из nodesList и сбрасываем выделение ---
            if (idx in nodesList.indices) {
                nodesList.removeAt(idx)
            }
            selectedNodeIndex = null
        }
        memorizedVertex = 0
    }


    /**
     * Изменяет цвет выделенного узла.
     *
     * @param color новый цвет (ARGB Int).
     */
    fun paintSelectedNode(color: Int) {
        selectedNodeIndex?.let { idx ->
            nodesList[idx] = nodesList[idx].copy(color = color)
        }
    }

    /**
     * Меняет цвет всех узлов на переданный.
     *
     * @param color цвет (ARGB Int) для всех узлов.
     */
    fun paintAll(color: Int) {
        nodesList.replaceAll { it.copy(color = color) }
    }

    /**
     * Сохраняет текущие координаты и цвета вершин из UI обратно в модель и затем в базу SQLite.
     *
     * @param graphId идентификатор графа в базе (по умолчанию = 1).
     */
    fun saveGraph(graphId: Int = 1) {
        nodesList.forEachIndexed { i, node ->
            val v = graph.getVertexes()[i]
            v.x = node.offset.x.toDouble()
            v.y = node.offset.y.toDouble()
            v.color = node.color
        }
        db.saveGraph(graph, graphId)
    }

    /**
     * Загружает граф из SQLite (id = graphId), очищает UI-список и заполняет его новыми CircleNode.
     *
     * @param graphId идентификатор графа (по умолчанию = 1).
     */
    fun loadGraph(graphId: Int = 1) {
        graph = db.loadGraph(graphId)
        nodesList.clear()
        graph.getVertexes().forEach { v ->
            nodesList.add(CircleNode(Offset(v.x.toFloat(), v.y.toFloat()), color = v.color))
        }
        selectedNodeIndex = null
        memorizedVertex = 0
    }


    fun loadNeo4jGraph(graphId: Int = 1) {
        try {
            graph = neo4jDb.readGraph(graphId)
            errorNeo4j = 0
        } catch (e: Exception){
            errorNeo4j = 1
        }
        nodesList.clear()
        graph.getVertexes().forEach { v ->
            nodesList.add(CircleNode(Offset(v.x.toFloat(), v.y.toFloat()), color = v.color))
        }
        selectedNodeIndex = null
        memorizedVertex = 0
    }

    fun saveNeo4jGraph(graphId: Int = 1) {
        nodesList.forEachIndexed { i, node ->
            val v = graph.getVertexes()[i]
            v.x = node.offset.x.toDouble()
            v.y = node.offset.y.toDouble()
            v.color = node.color
        }
        try{
        neo4jDb.saveGraphNeo4j(graph, graphId)
            errorNeo4j = 0
        } catch (e: Exception){
            errorNeo4j = 1
        }

    }


    fun loadCSVGraph() {
        var csv = RWCSV()
        graph = csv.read()
        nodesList.clear()
        graph.getVertexes().forEach { v ->
            nodesList.add(CircleNode(Offset(v.x.toFloat(), v.y.toFloat()), color = v.color))
        }
        selectedNodeIndex = null
        memorizedVertex = 0

    }

    fun saveCSVGraph() {
        var csv = RWCSV()
        nodesList.forEachIndexed { i, node ->
            val v = graph.getVertexes()[i]
            v.x = node.offset.x.toDouble()
            v.y = node.offset.y.toDouble()
            v.color = node.color
        }
        csv.write(graph)

    }

    /**
     * Начало перетаскивания: проверяет, если клик попал на узел, то сохраняет его индекс в activeDragIndex,
     * иначе возвращает false, и перетаскивание будет трактоваться как панорамирование канваса.
     *
     * @param pos экранная позиция касания.
     * @param padding отступ от границ канваса.
     * @return true, если началось перетаскивание узла, false — если канвас.
     */
    fun startDrag(pos: Offset, padding: Float): Boolean {
        val world = (pos / zoom) + pan + Offset(padding, padding)
        activeDragIndex = nodesList.indexOfFirst { (it.offset - world).getDistance() < it.radius }
            .takeIf { it != -1 }
        return activeDragIndex != null
    }

    /**
     * Обработка перетаскивания: если перетаскивается узел, меняем его координаты и синхронизируем с моделью,
     * иначе двигаем канвас (pan).
     *
     * @param delta смещение курсора от предыдущей позиции (в локальных координатах экрана).
     */
    fun onDrag(delta: Offset) {
        activeDragIndex?.let { idx ->
            // 1) Считаем новую позицию узла в координатах «мира»
            val oldOffset = nodesList[idx].offset
            val newOffset = oldOffset + (delta / zoom)

            // 2) Обновляем UI-узел
            nodesList[idx] = nodesList[idx].copy(offset = newOffset)

            // 3) Синхронизируем модель (Vertex хранит Double)
            val vertex = graph.getVertexes()[idx]
            vertex.x = newOffset.x.toDouble()
            vertex.y = newOffset.y.toDouble()

        } ?: run {
            // Перетаскиваем канвас
            pan -= delta / zoom
        }
    }

    /**
     * Альтернатива onDrag, когда нужно ограничить движение узла рамками канваса.
     *
     * @param idx индекс узла, который двигаем.
     * @param delta смещение курсора.
     * @param padding отступ от краёв.
     * @param canvasSize размер канваса.
     * @param zoom текущий коэффициент масштабирования.
     */
    fun onDragForNode(idx: Int, delta: Offset, padding: Float, canvasSize: IntSize, zoom: Float) {
        val old = nodesList[idx].offset
        val worldDelta = delta / zoom
        val candidate = old + worldDelta

        val minX = pan.x + padding
        val minY = pan.y + padding
        val maxX = minX + canvasSize.width / zoom
        val maxY = minY + canvasSize.height / zoom

        val clampedX = candidate.x.coerceIn(minX, maxX)
        val clampedY = candidate.y.coerceIn(minY, maxY)
        val deltaClamped = Offset(clampedX, clampedY) - old

        onDrag(deltaClamped * zoom)
    }

    /**
     * Завершает перетаскивание (сбрасывает activeDragIndex).
     */
    fun endDrag() {
        activeDragIndex = null
    }

    /**
     * Меняет масштаб канваса. Пересчитывает pan так, чтобы точка focus оставалась «на месте».
     *
     * @param factor множитель масштаба (например, 1.1f — увеличить, 0.9f — уменьшить).
     * @param focus экранные координаты точки, вокруг которой масштабируем.
     */
    fun zoomBy(factor: Float, focus: Offset) {
        val old = zoom
        val newZoom = (old * factor).coerceIn(0.1f, 5f)
        pan += (focus / old) - (focus / newZoom)
        zoom = newZoom
    }

    /**
     * Выполняет один шаг алгоритма ForceAtlas2: обновляет модель и синхронизирует UI-координаты.
     *
     * После вызова все узлы в nodesList получают новые координаты из vertexes.
     */
    fun applyForceAtlas2Layout() {
        // 1) Применяем раскладку к модели
        forceAtlas.applyLayout(graph)
        // 2) Синхронизируем UI: обновляем координаты узлов
        val vertices = graph.getVertexes()
        for (i in vertices.indices) {
            val v = vertices[i]
            nodesList[i] = nodesList[i].copy(offset = Offset(v.x.toFloat(), v.y.toFloat()))
        }
    }

    /**
     * Находит сильно связные компоненты (только для ориентированного графа)
     * и красит каждую компоненту в «случайный», но детерминированный цвет
     * (фиксированный seed для Random(0)).
     */
    fun highlightStronglyConnectedComponents() {
        if (graph.getType() != GraphType.ORIENTED) return
        val components = sccFinder.findComponents(graph)
        val rnd = Random(0)
        val colors = components.map {
            val rgb = rnd.nextInt(0x1_000_000)
            (0xFF shl 24) or rgb
        }

        val nodeCount = nodesList.size
        components.forEachIndexed { idx, comp ->
            val color = colors[idx]
            comp.forEach { nodeIndex ->
                if (nodeIndex in 0 until nodeCount) {
                    nodesList[nodeIndex] = nodesList[nodeIndex].copy(color = color)
                }
            }
        }
    }

    /**
     * Алгоритм Крускала: строит минимальное остовное дерево (MST) из алгоритма mstBuilder,
     * затем заменяет списки смежности на новые (только с ребрами MST), делает
     * ~100 итераций ForceAtlas2 для «распрямления», и обновляет UI-координаты.
     */
    fun highlightMinimumSpanningTree() {
        val mst: List<MinimumSpanningTree.MSTEdge> = mstBuilder.buildMST(graph)
        val vertexCount = graph.getVertexes().size
        val newAdjLists: MutableList<MutableList<Edge>> = MutableList(vertexCount) { mutableListOf() }
        val mstEdgeColor = 0xFFFF0000.toInt()

        for (e in mst) {
            newAdjLists[e.u].add(Edge(vertex = e.v, weight = e.weight, color = mstEdgeColor))
            newAdjLists[e.v].add(Edge(vertex = e.u, weight = e.weight, color = mstEdgeColor))
        }

        graph.getEdges().clear()
        newAdjLists.forEach { adj -> graph.getEdges().add(adj) }

        repeat(100) { forceAtlas.applyLayout(graph) }

        val vertices = graph.getVertexes()
        for (i in vertices.indices) {
            val v = vertices[i]
            nodesList[i] = nodesList[i].copy(offset = Offset(v.x.toFloat(), v.y.toFloat()))
        }
    }


    /**
     * Цвета сообществ определяются примерно согласно формуле:
     * CNT - количество сообществ
     * L - номер сообщества
     * A = FF
     * R = 255
     * G = L*255/CNT
     * B = 255 - L*255/CNT
     */
    fun dlpa() {
        val dlpa = DLPA(graph)
        var colors = mutableSetOf<Int>()
        dlpa.labelPropagation()
        for (elem in dlpa.labels) {
            colors.add(elem)
        }
        var vertexes = graph.getVertexes()
        for (i in 0..vertexes.size - 1) {
            var colorCNST = dlpa.labels[i]
            //vertexes[i].color = ((colorCNST*255/(colors.size.toDouble())).toInt()* 65536)
            vertexes[i].color = 255 * 65536
            vertexes[i].color += ((colorCNST * 255 / (colors.size.toDouble())).toInt() * 256)
            vertexes[i].color += (255 - (colorCNST * 255 / (colors.size.toDouble())).toInt())
            vertexes[i].color = -vertexes[i].color

        }
        val vertices = graph.getVertexes()
        for (i in vertices.indices) {
            val v = vertices[i]
            nodesList[i] = nodesList[i].copy(color = v.color)
        }
        selectedNodeIndex = null
    }

    /**
     * Цвета циклов определяются примерно согласно формуле:
     * CNT - количество циклов
     * L - номер цикла
     * A = FF
     * R = L*255/CNT
     * G = 255
     * B = 255 - L*255/CNT
     */
    fun searchCyles() {
        var choosenvertex = selectedNodeIndex ?: 0
        var cycles = SearchCycles(graph, choosenvertex).search()
        var edges = graph.getEdges()
        val defaultEdgeColor = 0xFF888888.toInt()
        graph.getEdges().forEach { list ->
            list.forEach { it.color = defaultEdgeColor }
        }
        var EdgeColor = 0xFFFF0000.toInt()
        var colors = -1
        for (cycle in cycles) {
            colors += 1
            EdgeColor = ((colors * 255 / (cycles.size.toDouble())).toInt() * 65536)
            EdgeColor += 255 * 256
            EdgeColor += (255 - (colors * 255 / (cycles.size.toDouble())).toInt())
            EdgeColor = -EdgeColor
            var start = choosenvertex
            for (vertex in cycle) {
                for (i in 0..edges[start].size - 1) {
                    if (edges[start][i].vertex == vertex) {
                        edges[start][i].color = EdgeColor
                        start = vertex
                        break
                    }
                }
            }
        }
        selectedNodeIndex = null
    }

    fun clearEdges() {
        val defaultEdgeColor = 0xFF888888.toInt()
        graph.getEdges().forEach { list ->
            list.forEach { it.color = defaultEdgeColor }
        }
    }
    fun copyGraph(graph: Graph):Graph{
        val newGraph = Graph(GraphType.NON_ORIENTED)
        for (i in graph.getEdges().indices){
            for (k in graph.getEdges()[i]){
                newGraph.addEdge(i,k.vertex,0,0)
            }
        }
        for (i in graph.getVertexes()){
            newGraph.addVertex(i.x,i.y,i.color)
        }
        return newGraph

    }
    fun collectiveInfluenceAlg(){
        val copy = copyGraph(graph)
        val nodes = CollectiveInfluence(copy)
        val result = nodes.getResultCollectiveInfluence(distance = 2)
        for (i in result){
            graph.getVertexes()[i].color = 0xFFFF0000.toInt()
        }
        val vertices = graph.getVertexes()
        for (i in vertices.indices) {
            val v = vertices[i]
            nodesList[i] = nodesList[i].copy(color = v.color)
        }


    }

    fun bridgeSearchAlg() {
        val defaultEdgeColor = 0xFF888888.toInt()
        graph.getEdges().forEach { list ->
            list.forEach { it.color = defaultEdgeColor }
        }
        val edges = graph.getEdges()
        val bridge = BridgeSearch(graph)
        val bridges = bridge.bridge()
        val edgeColor = 0xFFFFFF00.toInt()
        for (values in bridges) {
            edges[values.first].forEach { edge ->
                if (edge.vertex == values.second) {
                    edge.color = edgeColor
                }
            }
        }
    }

    fun DijkstraAlgorithm() {
        val edges = graph.getEdges()
        val selectedVertex = selectedNodeIndex ?: 0
        if (memorizedVertex > nodesList.size) {
            memorizedVertex = 0
        }
        val defaultEdgeColor = 0xFF888888.toInt()
        graph.getEdges().forEach { list ->
            list.forEach { it.color = defaultEdgeColor }
        }
        val dijkstra = DijkstraAlgorithm(memorizedVertex, selectedVertex, graph)
        val shortPath = dijkstra.dijkstra(graph, dijkstra.arrayEdge())


        val edgeColor = 0xFF00FF00.toInt()
        val path = shortPath.second
        if (path.size < 2) return
        for (i in 0 until path.size - 1) {
            val u = path[i]
            val v = path[i + 1]
            val edgeForward = edges[u].firstOrNull { it.vertex == v }
            edgeForward?.color = edgeColor
            val edgeBackward = edges[v].firstOrNull { it.vertex == u }
            edgeBackward?.color = edgeColor


        }
        result = "${shortPath.first}"
        selectedNodeIndex = null
    }

    fun FordBelman() {
        var choosenvertex = selectedNodeIndex ?: 0

        if (memorizedVertex > nodesList.size) {
            memorizedVertex = 0
        }

        var edges = graph.getEdges()

        var path = FordBelmanShortPath(graph, memorizedVertex, choosenvertex).getShortestPath() ?: mutableListOf<Int>()

        val defaultEdgeColor = 0xFF888888.toInt()
        graph.getEdges().forEach { list ->
            list.forEach { it.color = defaultEdgeColor }
        }

        val EdgeColor = 0xFFFF0000.toInt()

        if (path.size > 1) {
            for (vertex in 0..path.size - 2) {
                var minimumEdgeWeight = 2147483647
                for (edgeID in 0..edges[path[vertex]].size - 1) {
                    if (edges[path[vertex]][edgeID].vertex == path[vertex + 1]) {
                        minimumEdgeWeight = min(minimumEdgeWeight, edges[path[vertex]][edgeID].weight)
                    }
                }
                for (edge in edges[path[vertex]]) {
                    if (edge.vertex == path[vertex + 1]) {
                        if (edge.weight == minimumEdgeWeight) {
                            edge.color = EdgeColor
                        }
                    }
                }
            }
        }

        selectedNodeIndex = null
    }

    /**
     * Запоминает номер выбранной вершины, по умолчанию 0
     */
    fun MemorizeSelectedNode() {
        memorizedVertex = selectedNodeIndex ?: 0
    }

    /**
     * Изменяет радиус выбранной вершины на заданное значение.
     *
     * Если индекс выбранной вершины (`selectedNodeIndex`) не равен null, то:
     * - Получаем текущий радиус вершины из списка `nodesList`.
     * - Прибавляем к нему переданное смещение `delta`.
     * - Ограничиваем итоговый радиус значениями от 5f до 100f.
     * - Обновляем объект вершины в `nodesList`, сохраняя остальные параметры без изменений.
     *
     * @param delta Разница в радиусе, которую нужно применить к текущему значению.
     */
    fun changeSelectedNodeRadius(delta: Float) {
        selectedNodeIndex?.let { idx ->
            val oldRadius = nodesList[idx].radius
            // Минимальный радиус 5f, максимальный — 100f
            val newRadius = (oldRadius + delta).coerceIn(5f, 100f)
            nodesList[idx] = nodesList[idx].copy(radius = newRadius)
        }
    }

    /**
     * Масштабирует радиусы всех вершин в графе с помощью заданного коэффициента.
     *
     * Для каждой вершины в `nodesList`:
     * - Вычисляется новый радиус как произведение текущего значения на `factor`.
     * - Ограничиваем итоговый радиус диапазоном от 5f до 100f.
     * - Заменяем вершину на копию с обновлённым радиусом.
     *
     * @param factor Коэффициент масштабирования. Значение >1 увеличит радиусы, <1 — уменьшит.
     */
    fun scaleAllNodeRadii(factor: Float) {
        nodesList.forEachIndexed { idx, node ->
            val newR = (node.radius * factor).coerceIn(5f, 100f)
            nodesList[idx] = node.copy(radius = newR)
        }
    }

    /**
     * Обновляет экземпляр ForceAtlas2Layout с новыми параметрами.
     *
     * Вызывается после изменения любого из параметров алгоритма, чтобы пересоздать
     * объекты с учётом новых констант:
     * - repulsion — сила отталкивания между узлами.
     * - attraction — сила притяжения.
     * - damping — коэффициент демпфирования.
     * - gravity — сила гравитации.
     * - maxDisplacement — ограничение на перемещение узла за итерацию.
     *
     * @param repulsion Новое значение коэффициента силы отталкивания.
     * @param attraction Новое значение коэффициента силы притяжения.
     * @param damping Новое значение коэффициента демпфирования.
     * @param gravity Новое значение силы гравитации.
     * @param maxDisplacement Максимальное смещение вершины за итерацию.
     */
    fun updateForceAtlasParams(
        repulsion: Double,
        attraction: Double,
        damping: Double,
        gravity: Double,
        maxDisplacement: Double
    ) {
        // Пересоздаём экземпляр ForceAtlas2Layout с обновлёнными константами
        forceAtlas = ForceAtlas2Layout(
            repulsionConstant = repulsion,
            attractionConstant = attraction,
            damping = damping,
            gravity = gravity,
            maxDisplacement = maxDisplacement
        )
    }

    /**
     * Возвращает текущие параметры алгоритма ForceAtlas в виде объекта ForceAtlasParams.
     *
     * Извлекает из существующего экземпляра forceAtlas значения:
     * - коэффициента отталкивания;
     * - коэффициента притяжения;
     * - демпфирования;
     * - гравитации;
     * - максимального смещения.
     *
     * @return Объект ForceAtlasParams с актуальными значениями всех пяти параметров.
     */
    fun getForceAtlasParams(): ForceAtlasParams {
        return ForceAtlasParams(
            repulsion = forceAtlas.getRepulsionConstant(),
            attraction = forceAtlas.getAttractionConstant(),
            damping = forceAtlas.getDamping(),
            gravity = forceAtlas.getGravity(),
            maxDisplacement = forceAtlas.getMaxDisplacement()
        )
    }
}
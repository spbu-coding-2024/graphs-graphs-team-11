package org.spb.project.presenter

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.spb.project.common.Edge
import org.spb.project.common.Graph
import org.spb.project.common.GraphType
import org.spb.project.model.CircleNode
import kotlin.random.Random

class CanvasPresenter {
    private var graph by mutableStateOf(Graph(GraphType.NON_ORIENTED))
    private val db = GraphDbHelper
    private val forceAtlas = ForceAtlas2Layout()
    private val sccFinder = StronglyConnectedComponents()
    val graphType: GraphType
        get() = graph.getType()

    // UI-список вершин
    private val nodesList = mutableStateListOf<CircleNode>()
    val circleNodes: List<CircleNode> get() = nodesList

    // Список рёбер из модели
    val edges: List<List<Edge>> get() = graph.getEdges()

    // Текущий выбранный узел (или null)
    var selectedNodeIndex by mutableStateOf<Int?>(null)
        private set

    private var activeDragIndex by mutableStateOf<Int?>(null)
    var zoom by mutableStateOf(1f); private set
    var pan by mutableStateOf(Offset.Zero); private set

    init {
        loadGraph()
    }

    fun selectNode(index: Int?) {
        selectedNodeIndex = index
    }

    /**
     * Добавить вершину.
     * Если узел выбран, добавляем её и соединяем с ним.
     */
    fun addCircle(color: Int) {
        val pos = if (selectedNodeIndex != null) {
            val sel = nodesList[selectedNodeIndex!!]
            sel.offset + Offset(50f, 50f)
        } else {
            Offset(500f, 800f)
        }

        // 1) модель
        graph.addVertex(pos.x.toDouble(), pos.y.toDouble(), color)
        val newIdx = graph.getVertexes().lastIndex

        // если был выбран узел — добавить ребро
        selectedNodeIndex?.let { selIdx ->
            // вес = 1, цвет ребра можно по умолчанию
            val edgeColor = 0xFF888888.toInt()
            graph.addEdge(selIdx, newIdx, 1, edgeColor)
        }

        // 2) UI
        nodesList.add(CircleNode(pos, color = color))
    }

    /**
     * Удалить выбранный узел и все инцидентные рёбра.
     */
    fun deleteSelectedNode() {
        selectedNodeIndex?.let { idx ->
            // --- 1) модель: удаляем саму вершину ---
            if (idx in graph.getVertexes().indices) {
                graph.getVertexes().removeAt(idx)
            }
            // --- 2) модель: удаляем список исходящих рёбер этой вершины ---
            if (idx in graph.getEdges().indices) {
                graph.getEdges().removeAt(idx)
            }
            // --- 3) модель: пробегаем по всем оставшимся спискам рёбер ---
            graph.getEdges().forEach { list ->
                // удаляем все рёбра, ведущие в idx
                list.removeAll { it.vertex == idx }
                // теперь все вершины с большим индексом «сдвигаем» на 1
                for (i in list.indices) {
                    val e = list[i]
                    if (e.vertex > idx) {
                        // создаём новое ребро с уменьшенным vertex
                        list[i] = Edge(
                            vertex = e.vertex - 1,
                            weight = e.weight,
                            color = e.color
                        )
                    }
                }
            }

            // --- 4) UI: удаляем узел из списка и сбрасываем выделение ---
            if (idx in nodesList.indices) {
                nodesList.removeAt(idx)
            }
            selectedNodeIndex = null
        }
    }


    fun paintSelectedNode(color: Int) {
        selectedNodeIndex?.let { idx ->
            nodesList[idx] = nodesList[idx].copy(color = color)
        }
    }

    fun paintAll(color: Int) {
        nodesList.replaceAll { it.copy(color = color) }
    }

    fun saveGraph(graphId: Int = 1) {
        nodesList.forEachIndexed { i, node ->
            val v = graph.getVertexes()[i]
            v.x = node.offset.x.toDouble()
            v.y = node.offset.y.toDouble()
            v.color = node.color
        }
        db.saveGraph(graph, graphId)
    }

    fun loadGraph(graphId: Int = 1) {
        graph = db.loadGraph(graphId)
        nodesList.clear()
        graph.getVertexes().forEach { v ->
            nodesList.add(CircleNode(Offset(v.x.toFloat(), v.y.toFloat()), color = v.color))
        }
        selectedNodeIndex = null
    }

    /**
     * Начало перетаскивания узла/канвы.
     */
    fun startDrag(pos: Offset, padding: Float): Boolean {
        val world = (pos / zoom) + pan + Offset(padding, padding)
        activeDragIndex = nodesList.indexOfFirst { (it.offset - world).getDistance() < it.radius }
            .takeIf { it != -1 }
        return activeDragIndex != null
    }

    /**
     * Перетаскивание — либо конкретный узел, либо канвас.
     */
    fun onDrag(delta: Offset) {
        activeDragIndex?.let { idx ->
            // двигаем вершину
            nodesList[idx] = nodesList[idx].copy(offset = nodesList[idx].offset + (delta / zoom))
        } ?: run {
            // двигаем канвас
            pan -= delta / zoom
        }
    }

    /**
     * Альтернативный вариант: при ограничениях границ, если нужен.
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

    fun endDrag() {
        activeDragIndex = null
    }

    fun zoomBy(factor: Float, focus: Offset) {
        val old = zoom
        val newZoom = (old * factor).coerceIn(0.1f, 5f)
        pan += (focus / old) - (focus / newZoom)
        zoom = newZoom
    }

    /**
     * Вызывать после одного шага ForceAtlas2: обновляет модель и UI.
     */
    fun applyForceAtlas2Layout() {
        // 1) применяем раскладку к модели
        forceAtlas.applyLayout(graph)
        // 2) обновляем UI-координаты
        val vertices = graph.getVertexes()
        for (i in vertices.indices) {
            val v = vertices[i]
            nodesList[i] = nodesList[i].copy(offset = Offset(v.x.toFloat(), v.y.toFloat()))
        }
    }

    /**
     * Выделить сильносвязные компоненты: покрасить узлы в «случайные»,
     * но постоянно одни и те же цвета по компонентам.
     */
    fun highlightStronglyConnectedComponents() {
        // 1) SCC имеет смысл только для ориентированного графа
        if (graph.getType() != GraphType.ORIENTED) return

        // 2) Находим все СК-компоненты
        val components = sccFinder.findComponents(graph)

        // 3) Фиксируем сид, чтобы цветовые «рандомы» были всегда одинаковыми
        val rnd = Random(0)

        // 4) Для каждой компоненты генерируем свой цвет с альфой=FF
        val colors = components.map {
            val rgb = rnd.nextInt(0x1_000_000)         // диапазон 0x000000..0xFFFFFF
            (0xFF shl 24) or rgb                      // ARGB: непрозрачный цвет
        }

        // 5) Применяем цвет к вершинам, безопасно проверяя диапазон индексов
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
}
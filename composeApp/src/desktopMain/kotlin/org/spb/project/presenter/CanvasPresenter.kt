package org.spb.project.presenter

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import common.Graph
import common.GraphType
import org.spb.project.model.CircleNode

/**
 * Презентер, хранящий состояние узлов, pan/zoom и синхронизирующий модель ↔ UI ↔ БД.
 */
class CanvasPresenter {
    private var graph by mutableStateOf(Graph(GraphType.NORMAL))
    private val db = GraphDbHelper

    private val _nodes = mutableStateListOf<CircleNode>()
    val circleNodes: List<CircleNode> get() = _nodes

    private var activeDragIndex by mutableStateOf<Int?>(null)
    var zoom by mutableStateOf(1f); private set
    var pan  by mutableStateOf(Offset.Zero)

    init {
        loadGraph()
    }

    /**
     * Добавить вершину с заданным ARGB-цветом.
     * Цвет приходит из UI (selectedColor.toArgb()).
     */
    fun addCircle(color: Int) {
        val x = 500
        val y = 800
        graph.addVertex(x.toDouble(), y.toDouble(), color)
        _nodes.add(CircleNode(Offset(x.toFloat(), y.toFloat()), color = color))
    }

    /**
     * Перекрасить все вершины в указанный цвет.
     */
    fun paintAll(color: Int) {
        // Обновляем сразу UI-список
        _nodes.replaceAll { node ->
            node.copy(color = color)
        }
    }

    /**
     * Сохранить граф в БД:
     * синхронизировать координаты и цвета из UI в модель и вызвать GraphDbHelper.saveGraph.
     */
    fun saveGraph(graphId: Int = 1) {
        _nodes.forEachIndexed { idx, node ->
            val v = graph.getVertexes()[idx]
            v.x     = node.offset.x.toDouble()
            v.y     = node.offset.y.toDouble()
            v.color = node.color
        }
        db.saveGraph(graph, graphId)
    }

    /**
     * Загрузить граф из БД:
     * заменить модель, затем заполнить UI-список с учётом цвета.
     */
    fun loadGraph(graphId: Int = 1) {
        graph = db.loadGraph(graphId)
        _nodes.clear()
        graph.getVertexes().forEach { v ->
            _nodes.add(CircleNode(Offset(v.x.toFloat(), v.y.toFloat()), color = v.color))
        }
    }

    // === drag & zoom ===
    fun startDrag(pos: Offset, padding: Float): Boolean {
        val world = (pos / zoom) + pan + Offset(padding, padding)
        activeDragIndex =
            _nodes.indexOfFirst { (it.offset - world).getDistance() < it.radius }
                .takeIf { it != -1 }
        return activeDragIndex != null
    }

    fun onDrag(delta: Offset) {
        activeDragIndex?.let { idx ->
            val node = _nodes[idx]
            val newOff = node.offset + (delta / zoom)
            _nodes[idx] = node.copy(offset = newOff)
        } ?: run {
            pan -= delta / zoom
        }
    }

    fun endDrag() {
        activeDragIndex = null
    }

    fun zoomBy(factor: Float, focus: Offset) {
        val old = zoom
        val new = (old * factor).coerceIn(0.1f, 5f)
        pan += (focus / old) - (focus / new)
        zoom = new
    }
}

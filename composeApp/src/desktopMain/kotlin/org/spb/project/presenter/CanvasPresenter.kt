package org.spb.project.presenter

import org.spb.project.model.CircleNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import common.Graph
import common.GraphType

// Презентер для взаимодействия с UI: хранит состояние узлов, масштабирования и панорамирования канвы
class CanvasPresenter {
    private val db = GraphDbHelper

    // теперь наблюдаемый список
    private val circleNodeList = mutableStateListOf<CircleNode>()
    val circleNodes: List<CircleNode> get() = circleNodeList

    // graph — var, чтобы можно было перезатыкать его при загрузке
    private var graph: Graph = Graph(GraphType.WEIGHTED)

    init {
        // при старте сразу пробуем загрузить сохранённое состояние
        loadGraph()
    }

    // Индекс текущего перетаскиваемого узла, или null, если перетаскивание не активно
    private var activeDragIndex by mutableStateOf<Int?>(null)

    // Текущий масштаб канвы; приватный сеттер, чтобы изменение только через контролируемые методы
    var zoom by mutableStateOf(1f)
        private set

    // Смещение канвы (панорама) в мировых координатах
    var pan by mutableStateOf(Offset.Zero)

    // Добавляет новый узел в центр области (примерная позиция)
    fun addCircle() {
        val x = 500
        val y = 800
                // сначала модель
            graph.addVertex(x.toDouble(), y.toDouble())
        // потом UI-список
        circleNodeList.add(CircleNode(Offset(x.toFloat(), y.toFloat())))
    }

    fun saveGraph(graphId: Int = 1) {
        // синхронизируем координаты из UI в модель
        circleNodeList.forEachIndexed { idx, node ->
            val vertex = graph.getVertexes()[idx]
            vertex.x = node.offset.x.toDouble()
            vertex.y = node.offset.y.toDouble()
        }
        // сохраняем уже «правильную» модель
        GraphDbHelper.saveGraph(graph, graphId)
    }


    fun loadGraph(graphId: Int = 1) {
        // 1) заменяем нашу модель на загруженную
        graph = GraphDbHelper.loadGraph(graphId)

        // 2) очищаем UI-список и заполняем его из новой модели
        circleNodeList.clear()
        graph.getVertexes().forEach { v ->
            circleNodeList.add(
                CircleNode(Offset(v.x.toFloat(), v.y.toFloat()))
            )
        }
    }




    /**
     * Начало перетаскивания: определяем, какой узел попал под палец/мышь
     * pos — экранные координаты события,
     * padding — отступ внутренней области канвы в пикселях
     * Возвращаем true, если перетаскивание узла началось
     */
    fun startDrag(pos: Offset, padding: Float): Boolean {
        // Преобразуем экранные координаты в мировые (с учётом зума, панорамы и отступов)
        val world = (pos / zoom) + Offset(padding, padding) + pan
        // Ищем первый узел, расстояние до центра которого меньше радиуса
        activeDragIndex =
            circleNodeList.indexOfFirst { (it.offset - world).getDistance() < it.radius }.takeIf { it != -1 }
        return activeDragIndex != null
    }

    /**
     * Обработка перемещения во время перетаскивания
     * delta — изменение позиции курсора в экранных координатах
     */
    fun onDrag(delta: Offset) {
        activeDragIndex?.let { idx ->
            // Если перетаскиваем узел, двигаем его в мировых координатах
            val node = circleNodeList[idx]
            val newOff = node.offset + (delta / zoom)
            circleNodeList[idx] = node.copy(offset = newOff)
        } ?: run {
            // Иначе двигаем саму панораму канвы
            pan -= delta / zoom
        }
    }

    // Завершаем любое активное перетаскивание
    fun endDrag() {
        activeDragIndex = null
    }

    /**
     * Изменяем масштаб канвы с фокусом на указанной точке
     * factor — множитель масштабирования,
     * focus — точка в экранных координатах, вокруг которой происходит зум
     */
    fun zoomBy(factor: Float, focus: Offset) {
        val oldZoom = zoom
        // Ограничиваем масштаб, чтобы он не стал слишком маленьким или слишком большим
        val newZoom = (oldZoom * factor).coerceIn(0.1f, 5f)
        // Корректируем панораму так, чтобы фокус оставался на месте
        pan += (focus / oldZoom) - (focus / newZoom)
        zoom = newZoom
    }


    private fun Graph.toCircleNodeList(): SnapshotStateList<CircleNode> {
        val list = mutableStateListOf<CircleNode>()
        // Проходим по всем вершинам и создаём CircleNode из их координат
        this.getVertexes().forEach { vertex ->
            // vertex.x и vertex.y у вас Double, а Offset ожидает Float
            list.add(
                CircleNode(
                    Offset(
                        vertex.x.toFloat(),
                        vertex.y.toFloat()
                    )
                )
            )
        }
        return list
    }
}
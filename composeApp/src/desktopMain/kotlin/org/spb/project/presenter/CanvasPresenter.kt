package org.spb.project.presenter

import org.spb.project.model.CircleNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

// Презентер для взаимодействия с UI: хранит состояние узлов, масштабирования и панорамирования канвы
class CanvasPresenter {
    // Список узлов для отображения — Compose автоматически обновит View при изменениях
    private val circleNodeList = mutableStateListOf(
        CircleNode(Offset(300f, 300f)),  // начальные две точки для примера
        CircleNode(Offset(600f, 500f))
    )

    // Открытая неизменяемая копия списка, чтобы View не мог напрямую менять коллекцию
    val circleNodes: List<CircleNode> get() = circleNodeList

    // Индекс текущего перетаскиваемого узла, или null, если перетаскивание не активно
    private var activeDragIndex by mutableStateOf<Int?>(null)

    // Текущий масштаб канвы; приватный сеттер, чтобы изменение только через контролируемые методы
    var zoom by mutableStateOf(1f)
        private set

    // Смещение канвы (панорама) в мировых координатах
    var pan by mutableStateOf(Offset.Zero)

    // Добавляет новый узел в центр области (примерная позиция)
    fun addCircle() {
        circleNodeList.add(CircleNode(Offset(500f, 800f)))
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
}
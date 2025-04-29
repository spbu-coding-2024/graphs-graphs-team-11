import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

class CanvasPresenter {
    private val circleNodeList = mutableStateListOf(
        CircleNode(Offset(300f, 300f)),
        CircleNode(Offset(600f, 500f))
    )
    val circleNodes: List<CircleNode> get() = circleNodeList

    private var activeDragCircleIndex by mutableStateOf<Int?>(null)
    val activeDragIndex: Int? get() = activeDragCircleIndex

    var zoomScale by mutableStateOf(1f)
        private set

    // Увеличение масштаба канваса
    fun zoomIn() {
        zoomScale = (zoomScale + 0.05f).coerceAtMost(5f)  // Максимальное увеличение 5x
    }

    // Уменьшение масштаба канваса
    fun zoomOut() {
        zoomScale = (zoomScale - 0.05f).coerceAtLeast(0.05f) // Минимальный масштаб 0.05x (5% от размера)
    }

    // Добавление нового круга в канвас
    fun addCircle() {
        circleNodeList.add(CircleNode(Offset(500f, 800f)))
    }

    // Начать перетаскивание круга, если нажата внутренняя область круга
    fun startDrag(touchPosition: Offset, paddingPx: Float): Boolean {
        val adjustedTouch = touchPosition / zoomScale + Offset(paddingPx, paddingPx)
        activeDragCircleIndex = circleNodeList.indexOfFirst { circle ->
            (circle.offset - adjustedTouch).getDistance() < circle.radius
        }.takeIf { it != -1 }
        return activeDragCircleIndex != null
    }

    // Обработка перемещения круга
    fun drag(dragOffset: Offset, canvasSize: IntSize, paddingPx: Float) {
        activeDragCircleIndex?.let { index ->
            val circle = circleNodeList[index]
            val scaledDragOffset = dragOffset / zoomScale

            val updatedOffset = circle.offset + scaledDragOffset
            val circleRadius = circle.radius

            // Ограничение перемещения круга внутри канваса
            val minBoundary = paddingPx + circleRadius
            val maxXBoundary = canvasSize.width.toFloat() / zoomScale + paddingPx - circleRadius
            val maxYBoundary = canvasSize.height.toFloat() / zoomScale + paddingPx - circleRadius

            val clampedX = updatedOffset.x.coerceIn(minBoundary, maxXBoundary)
            val clampedY = updatedOffset.y.coerceIn(minBoundary, maxYBoundary)

            circleNodeList[index] = circle.copy(offset = Offset(clampedX, clampedY))
        }
    }

    // Завершение перетаскивания круга
    fun endDrag() {
        activeDragCircleIndex = null
    }
}
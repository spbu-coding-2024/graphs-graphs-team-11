import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun DraggableCanvasView(presenter: CanvasPresenter) {
    val circleNodes = presenter.circleNodes
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }

    val paddingDp = 10.dp
    val paddingPx = with(LocalDensity.current) { paddingDp.toPx() }

    val scales = circleNodes.mapIndexed { index, _ ->
        val targetScale = if (index == hoveredIndex) 1.2f else 1f
        animateFloatAsState(
            targetValue = targetScale,
            animationSpec = tween(durationMillis = 200)
        ).value
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.padding(16.dp)) {
            Button(onClick = { presenter.addCircle() }) {
                Text("Добавить круг")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { presenter.zoomIn() }) {
                Text("＋")
            }
            Spacer(Modifier.width(4.dp))
            Button(onClick = { presenter.zoomOut() }) {
                Text("－")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .border(2.dp, Color.LightGray)
        ) {
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingDp)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(circleNodes) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pos = event.changes.first().position
                                val adjusted = pos / presenter.zoomScale + Offset(paddingPx, paddingPx)
                                val idx = circleNodes.indexOfFirst {
                                    (it.offset - adjusted).getDistance() < it.radius
                                }
                                hoveredIndex = idx.takeIf { it != -1 }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touch ->
                                presenter.startDrag(touch, paddingPx)
                            },
                            onDrag = { _, dragAmount ->
                                presenter.drag(dragAmount, canvasSize, paddingPx)
                            },
                            onDragEnd = {
                                presenter.endDrag()
                            }
                        )
                    }
            ) {
                val s = presenter.zoomScale
                for (i in 0 until circleNodes.size - 1) {
                    drawLine(
                        color = Color.Gray,
                        start = (circleNodes[i].offset - Offset(paddingPx, paddingPx)) * s,
                        end = (circleNodes[i + 1].offset - Offset(paddingPx, paddingPx)) * s,
                        strokeWidth = 4f * s
                    )
                }

                circleNodes.forEachIndexed { index, circle ->
                    drawCircle(
                        color = Color.Blue,
                        radius = circle.radius * s * scales[index],
                        center = (circle.offset - Offset(paddingPx, paddingPx)) * s
                    )
                }
            }
        }
    }
}
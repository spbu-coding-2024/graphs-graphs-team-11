package org.spb.project.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clipToBounds
import org.spb.project.presenter.CanvasPresenter

// Верхняя панель с кнопками добавления окружности и управления зумом
@Composable
fun TopControlPanel(onAdd: () -> Unit, onZoomIn: () -> Unit, onZoomOut: () -> Unit) {
    Row(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxWidth()
            .background(Color(0xFFEFEFEF))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = onAdd) { Text("Добавить круг") }
        Button(onClick = onZoomIn) { Text("＋", fontSize = 20.sp) }
        Button(onClick = onZoomOut) { Text("－", fontSize = 20.sp) }
    }
}

// Боковая панель настроек: выбор цвета узлов
@Composable
fun LeftSidePanel(currentColor: Color, onColorSelected: (Color) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxHeight()
            .width(IntrinsicSize.Min)
            .background(Color(0xFFF0F0F0))
            .padding(50.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Настройки", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(16.dp))
        Text("Цвет узлов:")
        // Кнопка для раскрывающегося списка цветов
        OutlinedButton(onClick = { expanded = true }, Modifier.fillMaxWidth()) {
            Text(
                when (currentColor) {
                    Color.Blue -> "Синий"
                    Color.Red -> "Красный"
                    Color.Green -> "Зелёный"
                    Color.Yellow -> "Жёлтый"
                    else -> "Пользовательский"
                }
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        // Сама панель выбора цвета
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "Синий" to Color.Blue,
                "Красный" to Color.Red,
                "Зелёный" to Color.Green,
                "Жёлтый" to Color.Yellow
            ).forEach { (label, color) ->
                DropdownMenuItem(onClick = {
                    onColorSelected(color)
                    expanded = false
                }) { Text(label) }
            }
        }
    }
}

// Основная зона рисования: отображает узлы, линии, обрабатывает жесты
@Composable
fun DraggableCanvasView(
    presenter: CanvasPresenter,
    nodeColor: Color,
    modifier: Modifier = Modifier
) {
    val nodes = presenter.circleNodes
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }

    // Анимируем плавное изменение зума
    val animZoom by animateFloatAsState(presenter.zoom, tween(200))
    // Анимация при наведении на узел: увеличиваем размер
    val hoverScales = List(nodes.size) { idx ->
        animateFloatAsState(if (idx == hoverIndex) 1.2f else 1f, tween(200)).value
    }

    // Константы отступов для отрисовки канвы
    val paddingDp = 50.dp
    val paddingPx = with(LocalDensity.current) { paddingDp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .border(1.dp, Color.LightGray)
            .clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingDp)
                .onSizeChanged { canvasSize = it }
                // Обработка перемещения указателя для hover и скролла
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Move -> {
                                    val pos = event.changes.first().position
                                    val world = pos / presenter.zoom + presenter.pan + Offset(paddingPx, paddingPx)
                                    // Определяем, на каком узле сейчас наведение
                                    hoverIndex = nodes.indexOfFirst { (it.offset - world).getDistance() < it.radius }
                                        .takeIf { it != -1 }
                                }

                                PointerEventType.Scroll -> {
                                    val ch = event.changes.first()
                                    presenter.zoomBy(1f + ch.scrollDelta.y * 0.01f, ch.position)
                                    ch.consume()
                                }

                                else -> {}
                            }
                        }
                    }
                }
                // Жесты масштабирования и панорамы двумя пальцами
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        presenter.zoomBy(zoom, centroid)
                        presenter.pan -= pan / presenter.zoom
                    }
                }
                // Перетаскивание одного узла или панорамы
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { down ->
                            // Определяем, какой узел пытаются захватить
                            val worldStart = down / presenter.zoom + presenter.pan + Offset(paddingPx, paddingPx)
                            draggingIndex = nodes.indexOfFirst { (it.offset - worldStart).getDistance() < it.radius }
                                .takeIf { it != -1 }
                            presenter.startDrag(down, paddingPx)
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            presenter.onDrag(delta)
                            // Ограничиваем движение узла внутри видимой области
                            draggingIndex?.let { idx ->
                                val worldMin = presenter.pan + Offset(paddingPx, paddingPx)
                                val worldMax = worldMin + Offset(
                                    x = canvasSize.width / presenter.zoom,
                                    y = canvasSize.height / presenter.zoom
                                )
                                val node = nodes[idx]
                                node.offset = Offset(
                                    x = node.offset.x.coerceIn(worldMin.x, worldMax.x),
                                    y = node.offset.y.coerceIn(worldMin.y, worldMax.y)
                                )
                            }
                        },
                        onDragEnd = {
                            presenter.endDrag()
                            draggingIndex = null
                        }
                    )
                }
        ) {
            val z = animZoom
            val baseOffset = presenter.pan + Offset(paddingPx, paddingPx)
            // Рисуем линии между соседними узлами
            for (i in 0 until nodes.lastIndex) {
                drawLine(
                    Color.Gray,
                    start = (nodes[i].offset - baseOffset) * z,
                    end = (nodes[i + 1].offset - baseOffset) * z,
                    strokeWidth = 4f * z
                )
            }
            // Рисуем сами окружности с учётом зума и эффекта hover
            nodes.forEachIndexed { idx, node ->
                drawCircle(
                    color = nodeColor,
                    radius = node.radius * z * hoverScales[idx],
                    center = (node.offset - baseOffset) * z
                )
            }
        }
    }
}

// Обёртка экрана с топовой панелью, боковой панелью и самой канвой
@Composable
fun GraphScreen(presenter: CanvasPresenter) {
    var selectedColor by remember { mutableStateOf(Color.Blue) }
    Column(Modifier.fillMaxSize()) {
        TopControlPanel(
            onAdd = { presenter.addCircle() },
            onZoomIn = { presenter.zoomBy(1.05f, Offset.Zero) },
            onZoomOut = { presenter.zoomBy(0.95f, Offset.Zero) }
        )
        Row(Modifier.weight(1f)) {
            LeftSidePanel(currentColor = selectedColor, onColorSelected = { selectedColor = it })
            DraggableCanvasView(
                presenter = presenter,
                nodeColor = selectedColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(50.dp)
            )
        }
    }
}
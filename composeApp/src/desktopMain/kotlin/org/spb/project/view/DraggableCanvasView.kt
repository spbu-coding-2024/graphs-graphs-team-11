package org.spb.project.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import org.spb.project.presenter.CanvasPresenter
import org.spb.project.presenter.GraphDbHelper
import org.spb.project.presenter.GraphMeta

@Composable
fun TopControlPanel(
    onAdd: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onAdd)    { Text("Добавить круг") }
        Button(onClick = onZoomIn) { Text("＋") }
        Button(onClick = onZoomOut){ Text("－") }
        Button(onClick = onSave)   { Text("Сохранить граф") }
    }
}

@Composable
fun GraphManagerPanel(presenter: CanvasPresenter) {
    var graphList by remember { mutableStateOf(emptyList<GraphMeta>()) }
    var selected  by remember { mutableStateOf<GraphMeta?>(null) }
    var expanded  by remember { mutableStateOf(false) }

    // Загрузить список один раз
    LaunchedEffect(Unit) {
        graphList = GraphDbHelper.getAllGraphs()
    }

    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // выпадающий список
        Button(onClick = { expanded = true }) {
            Text(selected?.let { "Граф #${it.id} (${it.type})" } ?: "Выбрать граф")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            graphList.forEach { meta ->
                DropdownMenuItem(onClick = {
                    selected = meta
                    expanded = false
                }) {
                    Text("Граф #${meta.id} (${meta.type})")
                }
            }
        }

        Button(
            onClick = { selected?.let { presenter.loadGraph(it.id) } },
            enabled = selected != null
        ) {
            Text("Загрузить")
        }
        Button(onClick = {
            val newId = GraphDbHelper.getNextGraphId()
            presenter.saveGraph(newId)
            graphList = GraphDbHelper.getAllGraphs()
            selected  = graphList.find { it.id == newId }
        }) {
            Text("Сохранить как новый")
        }
    }
}

@Composable
fun ColorDropdown(
    current: Color,
    onSelect: (Color) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(Color.Blue to "Синий", Color.Red to "Красный", Color.Green to "Зелёный")

    Box(modifier = Modifier.padding(8.dp)) {
        Button(onClick = { expanded = true }) {
            Text(options.first { it.first == current }.second)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (color, label) ->
                DropdownMenuItem(onClick = {
                    onSelect(color)
                    expanded = false
                }) {
                    Text(label)
                }
            }
        }
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DraggableCanvasView(
    presenter: CanvasPresenter,
    nodeColor: Color,
    modifier: Modifier = Modifier,
    paddingDp: Dp = 50.dp
) {
    // Состояние
    val nodes = presenter.circleNodes
    var hoverIndex  by remember { mutableStateOf<Int?>(null) }
    var draggingIdx by remember { mutableStateOf<Int?>(null) }

    // Анимированный зум
    val animZoom by animateFloatAsState(
        targetValue = presenter.zoom,
        animationSpec = tween(200)
    )
    // Анимация увеличения при hover
    val hoverScales = nodes.mapIndexed { idx, _ ->
        animateFloatAsState(
            targetValue = if (idx == hoverIndex) 1.2f else 1f,
            animationSpec = tween(200)
        ).value
    }

    // Padding в px и размер канваса
    val density = LocalDensity.current
    val paddingPx = with(density) { paddingDp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(modifier = modifier
        // Узнаём размер
        .onSizeChanged { canvasSize = it }
        // 1) Drag- жесты (узел или панорама)
        .pointerInput(nodes) {
            detectDragGestures(
                onDragStart = { down ->
                    // Определяем, попали ли в узел
                    val world = (down / animZoom) + presenter.pan + Offset(paddingPx, paddingPx)
                    val idx = nodes.indexOfFirst {
                        (it.offset - world).getDistance() < it.radius
                    }.takeIf { it != -1 }
                    draggingIdx = idx
                    presenter.startDrag(down, paddingPx)
                },
                onDrag = { change, delta ->
                    change.consume()
                    if (draggingIdx != null) {
                        // ограничиваем движение узла
                        val idx = draggingIdx!!
                        val old = nodes[idx].offset
                        val worldDelta = delta / animZoom
                        val candidate = old + worldDelta

                        // вычисляем границы world-координат
                        val minX = presenter.pan.x + paddingPx
                        val minY = presenter.pan.y + paddingPx
                        val maxX = presenter.pan.x + paddingPx + canvasSize.width  / animZoom
                        val maxY = presenter.pan.y + paddingPx + canvasSize.height / animZoom

                        val clampedX = candidate.x.coerceIn(minX, maxX)
                        val clampedY = candidate.y.coerceIn(minY, maxY)
                        val clampedWorld = Offset(clampedX, clampedY)
                        // вычисляем фактический дельта
                        val deltaWorldClamped = (clampedWorld - old)
                        // переводим обратно в screen-дельту
                        presenter.onDrag(deltaWorldClamped * animZoom)
                    } else {
                        // панорамирование
                        presenter.onDrag(delta)
                    }
                },
                onDragEnd = {
                    presenter.endDrag()
                    draggingIdx = null
                }
            )
        }
        // 2) Pinch-to-zoom (для тач)
        .pointerInput(Unit) {
            detectTransformGestures { centroid, pan, zoomChange, _ ->
                presenter.zoomBy(zoomChange, centroid)
                presenter.onDrag(pan)
            }
        }
        // 3) Колесико мыши → зум
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val ev = awaitPointerEvent()
                    if (ev.type == PointerEventType.Scroll) {
                        ev.changes.forEach { ch ->
                            val delta = ch.scrollDelta
                            val factor = 1f + delta.y * 0.01f
                            presenter.zoomBy(factor, ch.position)
                            ch.consume()
                        }
                    }
                }
            }
        }
        // 4) Hover-эффект
        .pointerMoveFilter(
            onMove = { pos ->
                val world = (pos / animZoom) + presenter.pan + Offset(paddingPx, paddingPx)
                hoverIndex = nodes.indexOfFirst {
                    (it.offset - world).getDistance() < it.radius
                }.takeIf { it != -1 }
                false
            },
            onExit = {
                hoverIndex = null
                false
            }
        )
    ) {
        val z = animZoom
        val baseOffset = presenter.pan + Offset(paddingPx, paddingPx)

        // Рёбра (пример между соседними вершинами)
        for (i in 0 until nodes.size - 1) {
            drawLine(
                color       = Color.Gray,
                start       = (nodes[i].offset - baseOffset) * z,
                end         = (nodes[i + 1].offset - baseOffset) * z,
                strokeWidth = 2f * z
            )
        }
        // Узлы
        nodes.forEachIndexed { idx, node ->
            drawCircle(
                color  = nodeColor,
                radius = node.radius * z * hoverScales[idx],
                center = (node.offset - baseOffset) * z
            )
        }
    }
}

@Composable
fun GraphScreen(presenter: CanvasPresenter) {
    var selectedColor by remember { mutableStateOf(Color.Blue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp,
            backgroundColor = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Первая строчка кнопок: Добавить, +, –, Сохранить
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TopControlPanel(
                        onAdd    = { presenter.addCircle() },
                        onZoomIn = { presenter.zoomBy(1.05f, Offset.Zero) },
                        onZoomOut= { presenter.zoomBy(0.95f, Offset.Zero) },
                        onSave   = { presenter.saveGraph() }
                    )
                }

                // Менеджер сохранённых графов
                GraphManagerPanel(presenter)

                // Выбор цвета узлов
                ColorDropdown(
                    current  = selectedColor,
                    onSelect = { selectedColor = it }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp,
            backgroundColor = Color(0xFFFAFAFA),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFEFEF))
            ) {
                DraggableCanvasView(
                    presenter = presenter,
                    nodeColor = selectedColor,
                    modifier  = Modifier.fillMaxSize()
                )
            }
        }
    }
}
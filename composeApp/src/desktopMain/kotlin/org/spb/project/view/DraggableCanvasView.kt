package org.spb.project.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import org.spb.project.model.CircleNode
import org.spb.project.presenter.CanvasPresenter
import org.spb.project.presenter.GraphDbHelper
import org.spb.project.presenter.GraphMeta

@Composable
fun TopControlPanel(
    onAdd: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        Modifier
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

    LaunchedEffect(Unit) {
        graphList = GraphDbHelper.getAllGraphs()
    }

    Row(
        Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
        ) { Text("Загрузить") }
        Button(onClick = {
            val newId = GraphDbHelper.getNextGraphId()
            presenter.saveGraph(newId)
            graphList = GraphDbHelper.getAllGraphs()
            selected = graphList.find { it.id == newId }
        }) { Text("Сохранить как новый") }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DraggableCanvasView(
    presenter: CanvasPresenter,
    nodeColor: Color,               // выбранный цвет, передаётся в GraphScreen → onAdd
    modifier: Modifier = Modifier,
    paddingDp: Dp = 50.dp
) {
    val nodes = presenter.circleNodes
    var hoverIndex  by remember { mutableStateOf<Int?>(null) }
    var draggingIdx by remember { mutableStateOf<Int?>(null) }

    val animZoom    by animateFloatAsState(targetValue = presenter.zoom, animationSpec = tween(200))
    val hoverScales = nodes.mapIndexed { idx, _ ->
        animateFloatAsState(
            targetValue = if (idx == hoverIndex) 1.2f else 1f,
            animationSpec = tween(200)
        ).value
    }

    val density  = LocalDensity.current
    val paddingPx= with(density) { paddingDp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(modifier = modifier
        .onSizeChanged { canvasSize = it }
        .pointerInput(nodes) {
            detectDragGestures(
                onDragStart = { down ->
                    val world = (down / animZoom) + presenter.pan + Offset(paddingPx, paddingPx)
                    draggingIdx = nodes.indexOfFirst { (it.offset - world).getDistance() < it.radius }
                        .takeIf { it != -1 }
                    presenter.startDrag(down, paddingPx)
                },
                onDrag = { change, delta ->
                    change.consume()
                    if (draggingIdx != null) {
                        val idx = draggingIdx!!
                        val old = nodes[idx].offset
                        val worldDelta = delta / animZoom
                        val candidate = old + worldDelta

                        val minX = presenter.pan.x + paddingPx
                        val minY = presenter.pan.y + paddingPx
                        val maxX = minX + canvasSize.width  / animZoom
                        val maxY = minY + canvasSize.height / animZoom

                        val clampedX = candidate.x.coerceIn(minX, maxX)
                        val clampedY = candidate.y.coerceIn(minY, maxY)
                        val deltaClamped = (Offset(clampedX, clampedY) - old)

                        presenter.onDrag(deltaClamped * animZoom)
                    } else {
                        presenter.onDrag(delta)
                    }
                },
                onDragEnd = {
                    presenter.endDrag()
                    draggingIdx = null
                }
            )
        }
        .pointerInput(Unit) {
            detectTransformGestures { centroid, pan, zoomChange, _ ->
                presenter.zoomBy(zoomChange, centroid)
                presenter.onDrag(pan)
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val ev = awaitPointerEvent()
                    if (ev.type == PointerEventType.Scroll) {
                        ev.changes.forEach { ch ->
                            val factor = 1f + ch.scrollDelta.y * 0.01f
                            presenter.zoomBy(factor, ch.position)
                            ch.consume()
                        }
                    }
                }
            }
        }
        .pointerMoveFilter(
            onMove = { pos ->
                val world = (pos / animZoom) + presenter.pan + Offset(paddingPx, paddingPx)
                hoverIndex = nodes.indexOfFirst { (it.offset - world).getDistance() < it.radius }
                    .takeIf { it != -1 }
                false
            },
            onExit = {
                hoverIndex = null
                false
            }
        )
    ) {
        val z = animZoom
        val base = presenter.pan + Offset(paddingPx, paddingPx)

        // 1) Рёбра
        for (i in 0 until nodes.size - 1) {
            drawLine(
                color       = Color.Gray,
                start       = (nodes[i].offset - base) * z,
                end         = (nodes[i + 1].offset - base) * z,
                strokeWidth = 2f * z
            )
        }
        // 2) Вершины
        nodes.forEachIndexed { idx, node ->
            drawCircle(
                color  = Color(node.color),
                radius = node.radius * z * hoverScales[idx],
                center = (node.offset - base) * z
            )
        }
    }
}

@Composable
fun GraphScreen(presenter: CanvasPresenter) {
    // Список сохранённых графов
    var graphList by remember { mutableStateOf<List<GraphMeta>>(emptyList()) }
    // Выбранный граф
    var selected by remember { mutableStateOf<GraphMeta?>(null) }
    // Состояние дропа
    var expanded by remember { mutableStateOf(false) }
    // Выбранный цвет для новых вершин
    var selectedColor by remember { mutableStateOf(Color.Blue) }

    // Загрузим список графов из БД один раз при старте
    LaunchedEffect(Unit) {
        graphList = GraphDbHelper.getAllGraphs()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Панель управления ──
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp,
            backgroundColor = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Кнопки Add/Zoom/Save
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { presenter.addCircle(selectedColor.toArgb()) }) {
                        Text("Добавить вершину")
                    }
                    Button(onClick = { presenter.zoomBy(1.1f, Offset.Zero) }) {
                        Text("+")
                    }
                    Button(onClick = { presenter.zoomBy(0.9f, Offset.Zero) }) {
                        Text("-")
                    }
                    Button(onClick = { presenter.saveGraph() }) {
                        Text("Сохранить")
                    }
                }

                // Менеджер сохранённых графов
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dropdown для выбора графа
                    Box {
                        Button(onClick = { expanded = true }) {
                            Text(
                                selected?.let { "Граф #${it.id} (${it.type})" }
                                    ?: "Выбрать граф"
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            graphList.forEach { meta ->
                                DropdownMenuItem(onClick = {
                                    selected = meta
                                    expanded = false
                                }) {
                                    Text("Граф #${meta.id} (${meta.type})")
                                }
                            }
                        }
                    }

                    // Кнопка Загрузить
                    Button(
                        onClick = { selected?.let { presenter.loadGraph(it.id) } },
                        enabled = selected != null
                    ) {
                        Text("Загрузить")
                    }

                    // Кнопка Сохранить как новый
                    Button(onClick = {
                        val newId = GraphDbHelper.getNextGraphId()
                        presenter.saveGraph(newId)
                        graphList = GraphDbHelper.getAllGraphs()
                        // автоматически выбрать только что созданный
                        selected = graphList.firstOrNull { it.id == newId }
                    }) {
                        Text("Сохранить как новый")
                    }
                }

                // Dropdown для выбора цвета вершины
                ColorDropdown(
                    current  = selectedColor,
                    onSelect = { selectedColor = it }
                )
            }
        }

        // ── Canvas ──
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp,
            backgroundColor = Color(0xFFFAFAFA),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFEFEF))
            ) {
                DraggableCanvasView(
                    presenter = presenter,
                    modifier  = Modifier.fillMaxSize(),
                    nodeColor = selectedColor
                )
            }
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

    Box(Modifier.padding(8.dp)) {
        Button(onClick = { expanded = true }) {
            Text(options.first { it.first == current }.second)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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

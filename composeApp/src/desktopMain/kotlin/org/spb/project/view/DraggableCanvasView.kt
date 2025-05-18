package org.spb.project.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
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
import org.spb.project.presenter.CanvasPresenter
import org.spb.project.presenter.GraphDbHelper
import org.spb.project.presenter.GraphMeta

/**
 * Canvas с поддержкой:
 * - выбор вершины по клику
 * - отрисовки рёбер и вершин на основе модели
 * - hover-эффекта (увеличение)
 * - перетаскивания отдельных вершин и панорамы
 * - зума жестами и колесом мыши
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DraggableCanvasView(
    presenter: CanvasPresenter,
    modifier: Modifier = Modifier,
    paddingDp: Dp = 50.dp
) {
    val nodes = presenter.circleNodes
    val edges = presenter.edges
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    val selectedIndex by rememberUpdatedState(presenter.selectedNodeIndex)

    val zoom by rememberUpdatedState(presenter.zoom)
    val animatedZoom by animateFloatAsState(
        targetValue = zoom,
        animationSpec = tween(durationMillis = 200)
    )

    // Масштаб hover
    val hoverScales = List(nodes.size) { idx ->
        animateFloatAsState(
            targetValue = if (idx == hoverIndex) 1.2f else 1f,
            animationSpec = tween(200)
        ).value
    }

    val density = LocalDensity.current
    val paddingPx = with(density) { paddingDp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            // Тап для выбора вершины
            .pointerInput(nodes) {
                detectTapGestures { tap ->
                    val world = (tap / animatedZoom) + presenter.pan + Offset(paddingPx, paddingPx)
                    val idx = nodes.indexOfFirst { (it.offset - world).getDistance() < it.radius }
                        .takeIf { it != -1 }
                    presenter.selectNode(idx)
                }
            }
            // Drag для вершин или панорамы
            .pointerInput(nodes) {
                detectDragGestures(
                    onDragStart = { down ->
                        val world = (down / animatedZoom) + presenter.pan + Offset(paddingPx, paddingPx)
                        draggingIndex = nodes.indexOfFirst { (it.offset - world).getDistance() < it.radius }
                            .takeIf { it != -1 }
                        presenter.startDrag(down, paddingPx)
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        if (draggingIndex != null) {
                            presenter.onDragForNode(draggingIndex!!, delta, paddingPx, canvasSize, animatedZoom)
                        } else {
                            presenter.onDrag(delta)
                        }
                    },
                    onDragEnd = {
                        presenter.endDrag()
                        draggingIndex = null
                    }
                )
            }
            // Pinch zoom and pan
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val rawScroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (rawScroll != 0f) {
                                // Инвертируем исходный скролл, чтобы мышь и тачпад вели себя одинаково
                                val scroll = -rawScroll
                                // Положительный scroll => зум-ин, отрицательный => зум-аут
                                val factor = if (scroll > 0f) 1f + scroll / 100f else 1f / (1f + (-scroll / 100f))
                                presenter.zoomBy(factor, event.changes.first().position)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
            // Hover detection
            .pointerMoveFilter(
                onMove = { pos ->
                    val world = (pos / animatedZoom) + presenter.pan + Offset(paddingPx, paddingPx)
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
        val z = animatedZoom
        val base = presenter.pan + Offset(paddingPx, paddingPx)

        // 1) Рисуем рёбра по структуре graph.getEdges()
        edges.forEachIndexed { i, list ->
            val from = (nodes[i].offset - base) * z
            list.forEach { e ->
                val to = (nodes[e.vertex].offset - base) * z
                drawLine(
                    color = Color(e.color),
                    start = from,
                    end = to,
                    strokeWidth = 2f * z
                )
            }
        }

        // 2) Рисуем вершины; подсвечиваем выбранную
        nodes.forEachIndexed { idx, node ->
            val drawColor = if (idx == selectedIndex) Color.Yellow else Color(node.color)
            drawCircle(
                color = drawColor,
                radius = node.radius * z * hoverScales[idx],
                center = (node.offset - base) * z
            )
        }
    }
}


@Composable
fun GraphScreen(presenter: CanvasPresenter) {
    var graphList by remember { mutableStateOf<List<GraphMeta>>(emptyList()) }
    var selected by remember { mutableStateOf<GraphMeta?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Blue) }

    LaunchedEffect(Unit) {
        graphList = GraphDbHelper.getAllGraphs()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { presenter.addCircle(selectedColor.toArgb()) }) {
                        Text("Добавить вершину")
                    }
                    Button(onClick = { presenter.saveGraph() }) {
                        Text("Сохранить")
                    }
                    Button(onClick = { presenter.paintAll(selectedColor.toArgb()) }) {
                        Text("Окрасить все")
                    }
                    Button(
                        onClick = { presenter.paintSelectedNode(selectedColor.toArgb()) },
                        enabled = presenter.selectedNodeIndex != null
                    ) {
                        Text("Окрасить выбранную")
                    }
                    Button(
                        onClick = { presenter.deleteSelectedNode() },
                        enabled = presenter.selectedNodeIndex != null,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                    ) {
                        Text("Удалить выбранную", color = Color.White)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        Button(onClick = { expanded = true }) {
                            Text(selected?.let { "Граф #${it.id}" } ?: "Выбрать граф")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            graphList.forEach { meta ->
                                DropdownMenuItem(onClick = {
                                    selected = meta; expanded = false
                                }) { Text("Граф #${meta.id} (${meta.type})") }
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
                        selected = graphList.firstOrNull { it.id == newId }
                    }) { Text("Сохранить как новый") }
                    Button(
                        onClick = {
                            selected?.let {
                                GraphDbHelper.deleteGraph(it.id)
                                graphList = GraphDbHelper.getAllGraphs()
                                selected = null
                            }
                        }, enabled = selected != null,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                    ) {
                        Text("Удалить граф", color = Color.White)
                    }
                    ColorDropdown(current = selectedColor, onSelect = { selectedColor = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { presenter.applyForceAtlas2Layout() }) {
                        Text("Применить ForceAtlas2")
                    }
                    Button(onClick = { presenter.highlightStronglyConnectedComponents() }) {
                        Text("Выделить SCC")
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(Modifier.background(Color(0xFFEFEFEF))) {
                DraggableCanvasView(presenter = presenter, modifier = Modifier.fillMaxSize())
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
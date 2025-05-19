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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import org.spb.project.common.GraphType
import org.spb.project.presenter.CanvasPresenter
import org.spb.project.presenter.GraphDbHelper
import org.spb.project.presenter.GraphMeta
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
    showArrows: Boolean = true,
    showWeights: Boolean = true,
    modifier: Modifier = Modifier,
    paddingDp: Dp = 50.dp
) {
    // 1) Состояния и данные
    val nodes         = presenter.circleNodes
    val edges         = presenter.edges
    var hoverIndex    by remember { mutableStateOf<Int?>(null) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    val selectedIndex by rememberUpdatedState(presenter.selectedNodeIndex)

    // 2) Zoom + анимация
    val targetZoom   by rememberUpdatedState(presenter.zoom)
    val animatedZoom by animateFloatAsState(targetValue = targetZoom, animationSpec = tween(200))
    val z            = animatedZoom

    // 3) Панорама и padding
    val pan       = presenter.pan
    val density   = LocalDensity.current
    val paddingPx = with(density) { paddingDp.toPx() }

    // 4) Для onDragForNode
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 5) Единый base и конвертер экран→мир
    val base = pan + Offset(paddingPx, paddingPx)
    fun toWorld(pos: Offset) = pos / z + base

    Box(modifier = modifier.background(Color(0xFFEFEFEF))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }

                // 1) TAP — пересоздаётся при изменении pan или z
                .pointerInput(pan, z) {
                    detectTapGestures { tap ->
                        val world = toWorld(tap)
                        val idx = nodes.indexOfFirst {
                            (it.offset - world).getDistance() < it.radius
                        }.takeIf { it != -1 }
                        presenter.selectNode(idx)
                    }
                }

                // 2) DRAG — тоже с актуальными pan/z
                .pointerInput(pan, z) {
                    detectDragGestures(
                        onDragStart = { down ->
                            val world = toWorld(down)
                            draggingIndex = nodes.indexOfFirst {
                                (it.offset - world).getDistance() < it.radius
                            }.takeIf { it != -1 }
                            presenter.startDrag(down, paddingPx)
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            if (draggingIndex != null) {
                                presenter.onDragForNode(draggingIndex!!, delta, paddingPx, canvasSize, z)
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

                // 3) Pinch-to-zoom & two-finger pan (touchpad)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, panDelta, zoomDelta, _ ->
                        if (panDelta != Offset.Zero) presenter.onDrag(panDelta)
                        if (zoomDelta != 1f) {
                            val factor = 1f / zoomDelta
                            val worldCentroid = toWorld(centroid)
                            presenter.zoomBy(factor, worldCentroid)
                        }
                    }
                }

                // 4) Wheel & touchpad scroll: инверсия
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                if (ev.type == PointerEventType.Scroll) {
                                    val scroll = ev.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                    if (scroll != 0f) {
                                        val factor = if (scroll > 0f)
                                            1f / (1f + scroll / 100f)
                                        else
                                            1f + (-scroll / 100f)
                                        val worldPos = toWorld(ev.changes.first().position)
                                        presenter.zoomBy(factor, worldPos)
                                        ev.changes.forEach { it.consume() }
                                    }
                                } else {
                                    break
                                }
                            }
                        }
                    }
                }

                // 5) Hover
                .pointerMoveFilter(
                    onMove = { pos ->
                        val world = toWorld(pos)
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
            // 1) рисуем рёбра + накапливаем стрелочные головки
            val arrowHeads = mutableListOf<Pair<Path, Color>>()
            edges.forEachIndexed { i, list ->
                val from = (nodes[i].offset - base) * z
                list.forEach { e ->
                    val to = (nodes[e.vertex].offset - base) * z
                    drawLine(Color(e.color), from, to, strokeWidth = 2f * z)
                    if (showArrows && (presenter.graphType == GraphType.ORIENTED || presenter.graphType == GraphType.WEIGHTED_ORIENTED)) {
                        val angle = atan2(to.y - from.y, to.x - from.x)
                        val asz   = 10f * z
                        val aang  = (PI/6).toFloat()
                        val p = Path().apply {
                            moveTo(to.x, to.y)
                            lineTo(to.x - asz*cos(angle-aang), to.y - asz*sin(angle-aang))
                            moveTo(to.x, to.y)
                            lineTo(to.x - asz*cos(angle+aang), to.y - asz*sin(angle+aang))
                        }
                        arrowHeads += p to Color(e.color)
                    }
                }
            }

            // 2) рисуем узлы
            nodes.forEachIndexed { idx, node ->
                val scale  = if (idx == hoverIndex) 1.2f else 1f
                val radius = node.radius * z * scale
                val color  = if (idx == selectedIndex) Color.Yellow else Color(node.color)
                drawCircle(color, center = (node.offset - base) * z, radius = radius)
            }

            // 3) рисуем стрелочные головки
            if (showArrows && (presenter.graphType == GraphType.ORIENTED || presenter.graphType == GraphType.WEIGHTED_ORIENTED)) {
                arrowHeads.forEach { (path, color) ->
                    drawPath(path, color, style = Stroke(width = 2f * z))
                }
            }
        }

        // Overlay: Text-веса
        if (showWeights && (presenter.graphType == GraphType.WEIGHTED_ORIENTED || presenter.graphType == GraphType.WEIGHTED_NON_ORIENTED)) {
            edges.forEachIndexed { i, list ->
                val from = (nodes[i].offset - base) * z
                list.forEach { e ->
                    val to  = (nodes[e.vertex].offset - base) * z
                    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
                    val xDp = with(density) { mid.x.toDp() }
                    val yDp = with(density) { mid.y.toDp() }
                    Text(
                        text = e.weight.toString(),
                        fontSize = (14 * z).sp,
                        color = Color(e.color),
                        modifier = Modifier.offset { IntOffset(xDp.roundToPx(), yDp.roundToPx()) }
                    )
                }
            }
        }
    }
}

@Composable
fun GraphScreen(presenter: CanvasPresenter) {
    var graphList by remember { mutableStateOf<List<GraphMeta>>(emptyList()) }
    var selected by remember { mutableStateOf<GraphMeta?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Blue) }

    // Состояния чекбоксов
    var showArrows by remember { mutableStateOf(true) }
    var showWeights by remember { mutableStateOf(true) }

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
                    Button(onClick = { presenter.addCircle(selectedColor.toArgb()) }) { Text("Добавить вершину") }
                    Button(onClick = { presenter.saveGraph() }) { Text("Сохранить") }
                    Button(onClick = { presenter.paintAll(selectedColor.toArgb()) }) { Text("Окрасить все") }
                    Button(onClick = { presenter.paintSelectedNode(selectedColor.toArgb()) }, enabled = presenter.selectedNodeIndex != null) { Text("Окрасить выбранную") }
                    Button(onClick = { presenter.deleteSelectedNode() }, enabled = presenter.selectedNodeIndex != null, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) { Text("Удалить выбранную", color = Color.White) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        Button(onClick = { expanded = true }) { Text(selected?.let { "Граф #${it.id}" } ?: "Выбрать граф") }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            graphList.forEach { meta -> DropdownMenuItem(onClick = { selected = meta; expanded = false }) { Text("Граф #${meta.id} (${meta.type})") } }
                        }
                    }
                    Button(onClick = { selected?.let { presenter.loadGraph(it.id) } }, enabled = selected != null) { Text("Загрузить") }
                    Button(onClick = {
                        val newId = GraphDbHelper.getNextGraphId()
                        presenter.saveGraph(newId)
                        graphList = GraphDbHelper.getAllGraphs()
                        selected = graphList.firstOrNull { it.id == newId }
                    }) { Text("Сохранить как новый") }
                    Button(onClick = {
                        selected?.let { GraphDbHelper.deleteGraph(it.id); graphList = GraphDbHelper.getAllGraphs(); selected = null }
                    }, enabled = selected != null, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) { Text("Удалить граф", color = Color.White) }
                    ColorDropdown(current = selectedColor, onSelect = { selectedColor = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { presenter.applyForceAtlas2Layout() }) { Text("Применить ForceAtlas2") }
                    Button(onClick = { presenter.highlightStronglyConnectedComponents() }) { Text("Выделить SCC") }
                }
                // Чекбоксы для управления отображением
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Checkbox(checked = showArrows, onCheckedChange = { showArrows = it })
                    Text(text = "Отображать стрелки")
                    Spacer(modifier = Modifier.width(24.dp))
                    Checkbox(checked = showWeights, onCheckedChange = { showWeights = it })
                    Text(text = "Отображать веса")
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
                DraggableCanvasView(
                    presenter = presenter,
                    showArrows = showArrows,
                    showWeights = showWeights,
                    modifier = Modifier.fillMaxSize()
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
        Button(onClick = { expanded = true }) { Text(options.first { it.first == current }.second) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (color, label) ->
                DropdownMenuItem(onClick = { onSelect(color); expanded = false }) { Text(label) }
            }
        }
    }
}

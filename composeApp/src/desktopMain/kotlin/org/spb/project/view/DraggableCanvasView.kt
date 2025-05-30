package org.spb.project.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import org.spb.project.common.GraphType
import org.spb.project.presenter.CanvasPresenter
import org.spb.project.presenter.GraphDbHelper
import org.spb.project.presenter.GraphMeta
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.spb.project.presenter.DLPA
import org.spb.project.presenter.FordBelmanShortPath
import org.spb.project.presenter.SearchCycles

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
        // Текст текущего масштаба в правом нижнем углу
        Text(
            text = "Масштаб: ${(presenter.zoom * 100).toInt()}%",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color(0xAAFFFFFF), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Composable
fun GraphScreen(presenter: CanvasPresenter) {
    val selectedBg = Color(0xFF7C1DFF)
    val unselectedBg = Color(0xFFF5F5F5)
    val selectedText = Color.White
    val unselectedText = Color.Black

    var graphsExpanded by remember { mutableStateOf(false) }
    var algosExpanded by remember { mutableStateOf(false) }
    var loadExpanded by remember { mutableStateOf(false) }

    var graphList by remember { mutableStateOf<List<GraphMeta>>(emptyList()) }
    var selectedGraph by remember { mutableStateOf<GraphMeta?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("SQL") }
    var selectedColor by remember { mutableStateOf(Color.Blue) }
    var showArrows by remember { mutableStateOf(true) }
    var showWeights by remember { mutableStateOf(true) }

    val radioOptions = listOf("SQL", "NEO4J", "csv")

    LaunchedEffect(Unit) {
        graphList = GraphDbHelper.getAllGraphs()
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEFEFEF))
                .zIndex(0f)
        ) {
            DraggableCanvasView(
                presenter = presenter,
                showArrows = showArrows,
                showWeights = showWeights,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .zIndex(1f)
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Button(
                        onClick = {
                            graphsExpanded = !graphsExpanded
                            if (graphsExpanded) { algosExpanded = false; loadExpanded = false }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (graphsExpanded) selectedBg else unselectedBg,
                            contentColor = if (graphsExpanded) selectedText else unselectedText
                        )
                    ) { Text("Граф") }

                    Button(
                        onClick = {
                            algosExpanded = !algosExpanded
                            if (algosExpanded) { graphsExpanded = false; loadExpanded = false }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (algosExpanded) selectedBg else unselectedBg,
                            contentColor = if (algosExpanded) selectedText else unselectedText
                        )
                    ) { Text("Алгоритмы") }

                    Button(
                        onClick = {
                            loadExpanded = !loadExpanded
                            if (loadExpanded) { graphsExpanded = false; algosExpanded = false }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (loadExpanded) selectedBg else unselectedBg,
                            contentColor = if (loadExpanded) selectedText else unselectedText
                        )
                    ) { Text("Загрузка") }
                }
            }

            AnimatedVisibility(
                visible = graphsExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                GraphPanel(
                    presenter = presenter,
                    showArrows = showArrows,
                    onToggleArrows = { showArrows = it },
                    showWeights = showWeights,
                    onToggleWeights = { showWeights = it },
                    selectedColor = selectedColor,
                    onSelectColor = { selectedColor = it }
                )
            }

            AnimatedVisibility(
                visible = algosExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                AlgoPanel(presenter = presenter)
            }

            AnimatedVisibility(
                visible = loadExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                LoadPanel(
                    graphList = graphList,
                    selectedGraph = selectedGraph,
                    expandedDropdown = expandedDropdown,
                    onToggleDropdown = { expandedDropdown = it },
                    onSelectGraph = { selectedGraph = it },
                    radioOptions = radioOptions,
                    selectedOption = selectedOption,
                    onOptionSelected = { selectedOption = it },
                    presenter = presenter,
                    onRefreshList = { graphList = GraphDbHelper.getAllGraphs() }
                )
            }
        }
    }
}

@Composable
private fun GraphPanel(
    presenter: CanvasPresenter,
    showArrows: Boolean,
    onToggleArrows: (Boolean) -> Unit,
    showWeights: Boolean,
    onToggleWeights: (Boolean) -> Unit,
    selectedColor: Color,
    onSelectColor: (Color) -> Unit
) {

    Card(shape = RoundedCornerShape(8.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { presenter.addCircle(selectedColor.toArgb()) }) { Text("Добавить вершину") }
                Button(onClick = { presenter.paintAll(selectedColor.toArgb()) }) { Text("Окрасить все") }
                Button(onClick = { presenter.paintSelectedNode(selectedColor.toArgb()) }, enabled = presenter.selectedNodeIndex != null) { Text("Окрасить выбранную") }
                Button(onClick = { presenter.deleteSelectedNode() }, enabled = presenter.selectedNodeIndex != null,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) { Text("Удалить выбранную", color = Color.White) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { presenter.changeSelectedNodeRadius(5f) },
                    enabled = presenter.selectedNodeIndex != null
                ) { Text("Увеличить размер") }
                Button(
                    onClick = { presenter.changeSelectedNodeRadius(-5f) },
                    enabled = presenter.selectedNodeIndex != null
                ) { Text("Уменьшить размер") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    presenter.scaleAllNodeRadii(1.1f)
                }) {
                    Text("Увеличить все вершины")
                }
                Button(onClick = {
                    presenter.scaleAllNodeRadii(0.9f)
                }) {
                    Text("Уменьшить все вершины")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showArrows, onCheckedChange = onToggleArrows)
                Text("Отображать стрелки")
                Checkbox(checked = showWeights, onCheckedChange = onToggleWeights)
                Text("Отображать веса")
            }
            ColorDropdown(current = selectedColor, onSelect = onSelectColor)
        }
    }
}

@Composable
private fun AlgoPanel(presenter: CanvasPresenter) {

    Card(shape = RoundedCornerShape(8.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { presenter.applyForceAtlas2Layout() }) { Text("ForceAtlas2") }
                Button(onClick = { presenter.highlightStronglyConnectedComponents() }, enabled = presenter.graphType == GraphType.ORIENTED) { Text("SCC") }
                Button(onClick = { presenter.highlightMinimumSpanningTree() }, enabled = presenter.graphType == GraphType.WEIGHTED_NON_ORIENTED) { Text("MST") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { presenter.dlpa() }) { Text("DLPA") }
                Button(onClick = { presenter.searchCyles() }, enabled = presenter.selectedNodeIndex != null) { Text("Найти циклы") }
                Button(onClick = { presenter.FordBelman() }, enabled = presenter.selectedNodeIndex != null) { Text("Форд-Белман") }
                Button(onClick = { presenter.MemorizeSelectedNode() }, enabled = presenter.selectedNodeIndex != null) { Text("Запомнить вершину") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){
                Button(onClick = {presenter.bridgeSearchAlg()}){Text("Поиск мостов")}
                Button(onClick = {presenter.DijkstraAlgorithm()} , enabled = presenter.selectedNodeIndex!=null) {Text("Алгоритм Дейкстры")}
                Spacer(Modifier.width(16.dp))

            }

        }
        }
    }

@Composable
private fun LoadPanel(
    graphList: List<GraphMeta>,
    selectedGraph: GraphMeta?,
    expandedDropdown: Boolean,
    onToggleDropdown: (Boolean) -> Unit,
    onSelectGraph: (GraphMeta?) -> Unit,
    radioOptions: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    presenter: CanvasPresenter,
    onRefreshList: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Источник данных:")
            radioOptions.forEach { opt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = opt == selectedOption, onClick = { onOptionSelected(opt) })
                    Text(opt)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when (selectedOption) {
                        "NEO4J" -> selectedGraph?.let { presenter.saveNeo4jGraph(it.id) }
                        "csv" -> selectedGraph?.let { presenter.saveCSVGraph() }
                        else -> selectedGraph?.let { presenter.saveGraph(it.id) }
                    }
                }, enabled = selectedGraph != null) { Text("Сохранить") }
                Button(onClick = {
                    when (selectedOption) {
                        "NEO4J" -> selectedGraph?.let { presenter.loadNeo4jGraph(it.id) }
                        "csv" -> selectedGraph?.let { presenter.loadCSVGraph() }
                        else -> selectedGraph?.let { presenter.loadGraph(it.id) }
                    }
                }, enabled = selectedGraph != null) { Text("Загрузить") }
                Button(onClick = {
                    val newId = GraphDbHelper.getNextGraphId()
                    presenter.saveGraph(newId)
                    onRefreshList()
                    GraphDbHelper.getAllGraphs().firstOrNull { it.id == newId }?.let { onSelectGraph(it) }
                }) { Text("Сохранить как новый") }
                Button(onClick = {
                    selectedGraph?.let {
                        GraphDbHelper.deleteGraph(it.id)
                        onRefreshList()
                        onSelectGraph(null)
                    }
                }, enabled = selectedGraph != null, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)) {
                    Text("Удалить", color = Color.White)
                }
            }
            Box {
                Button(onClick = { onToggleDropdown(true) }) {
                    Text(selectedGraph?.let { "Граф #${it.id}" } ?: "Выбрать граф")
                }
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { onToggleDropdown(false) }) {
                    graphList.forEach { meta ->
                        DropdownMenuItem(onClick = { onSelectGraph(meta); onToggleDropdown(false) }) {
                            Text("Граф #${meta.id} (${meta.type})")
                        }
                    }
                }
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
    val options = listOf(
        Color.Blue to "Синий",
        Color.Red to "Красный",
        Color.Green to "Зелёный",
        Color.Yellow to "Жёлтый",
        Color.Cyan to "Голубой",
        Color.Magenta to "Пурпурный",
        Color.Gray to "Серый",
        Color.Black to "Чёрный",
        Color(0xFFFFA500) to "Оранжевый"
    )

    Box(Modifier.padding(8.dp)) {
        Button(onClick = { expanded = true }) { Text(options.first { it.first == current }.second) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (color, label) ->
                DropdownMenuItem(onClick = { onSelect(color); expanded = false }) { Text(label) }
            }
        }
    }
}
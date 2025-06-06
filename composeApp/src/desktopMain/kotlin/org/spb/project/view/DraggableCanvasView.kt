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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import org.spb.project.model.common.GraphType
import org.spb.project.presenter.ui.CanvasPresenter
import org.spb.project.presenter.database.GraphDbHelper
import org.spb.project.model.ui.GraphMeta
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Интерактивный Canvas для отображения и управления графом.
 *
 * Поддерживает:
 * - выбор вершины по клику;
 * - отрисовку рёбер и узлов на основе данных из Presenter;
 * - hover-эффект (легкое увеличение вершины при наведении курсора);
 * - перетаскивание отдельных вершин и панорамирование всего холста;
 * - масштабирование (зум) с помощью жестов (pinch-to-zoom) и колесика мыши;
 * - отображение стрелок для ориентированных рёбер и весов для взвешенных графов;
 * - динамическое изменение скорости зума с помощью ползунка.
 *
 * @param presenter Объект Presenter, отвечающий за синхронизацию с моделью графа и выполнение логики.
 * @param showArrows Флаг, включающий отрисовку стрелок на ориентированных рёбрах.
 * @param showWeights Флаг, отвечающий за вывод весов рёбер (для взвешенных графов).
 * @param modifier Модификатор Compose для настройки внешнего вида и поведения контейнера.
 * @param paddingDp Отступ (в dp) от краёв контейнера до рабочей области Canvas; используется при преобразовании координат.
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
    // 1) Получаем текущие списки узлов и рёбер из Presenter
    val nodes = presenter.circleNodes
    val edges = presenter.edges

    // Для эффекта hover (индекс узла под курсором) и перетаскивания (индекс активного узла)
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }

    // Ссылка на индекс выбранной вершины в Presenter (не изменяется при смене Presenter.selectedNodeIndex)
    val selectedIndex by rememberUpdatedState(presenter.selectedNodeIndex)

    // 2) Обработка зума: текущее значение, анимация и позиция ползунка скорости зума
    val targetZoom by rememberUpdatedState(presenter.zoom)
    val animatedZoom by animateFloatAsState(targetValue = targetZoom, animationSpec = tween(200))
    val z = animatedZoom
    var sliderPosition by remember { mutableStateOf(0f) }
    val maxZoomSpeed = 100f

    // 3) Панорамирование (pan) и отступ (padding) в пикселях
    val pan = presenter.pan
    val density = LocalDensity.current
    val paddingPx = with(density) { paddingDp.toPx() }

    // 4) Размер Canvas для ограничения перетаскивания узлов
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 5) Базовый оффсет для преобразования «мир→экран» с учётом pan и padding
    val base = pan + Offset(paddingPx, paddingPx)
    fun toWorld(pos: Offset): Offset = pos / z + base

    Box(modifier = modifier.background(Color(0xFFEFEFEF))) {
        Canvas(modifier = Modifier.fillMaxSize().onSizeChanged { canvasSize = it }
            // 1) Обработка клика (tap) для выбора вершины: пересоздаётся при изменении pan или z
            .pointerInput(pan, z) {
                detectTapGestures { tap ->
                    val world = toWorld(tap)
                    // Ищем индекс узла, по которому кликнули
                    val idx = nodes.indexOfFirst {
                        (it.offset - world).getDistance() < it.radius
                    }.takeIf { it != -1 }
                    presenter.selectNode(idx)
                }
            }

            // 2) Обработка перетаскивания (drag):
            //    - Если начат drag по узлу, меняем его координаты;
            //    - Иначе — панорамируем Canvas.
            .pointerInput(pan, z) {
                detectDragGestures(onDragStart = { down ->
                    val world = toWorld(down)
                    draggingIndex = nodes.indexOfFirst {
                        (it.offset - world).getDistance() < it.radius
                    }.takeIf { it != -1 }
                    presenter.startDrag(down, paddingPx)
                }, onDrag = { change, delta ->
                    change.consume()
                    if (draggingIndex != null) {
                        // Перетаскиваем конкретную вершину с ограничениями
                        presenter.onDragForNode(draggingIndex!!, delta, paddingPx, canvasSize, z)
                    } else {
                        // Панорамируем весь холст
                        presenter.onDrag(delta)
                    }
                }, onDragEnd = {
                    presenter.endDrag()
                    draggingIndex = null
                })
            }

            // 3) Pinch-to-zoom и два пальца для панорамирования (touchpad)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, panDelta, zoomDelta, _ ->
                    if (panDelta != Offset.Zero) {
                        presenter.onDrag(panDelta)
                    }
                    if (zoomDelta != 1f) {
                        // При изменении масштаба учитываем точку «фокуса» жеста
                        val factor = 1f / zoomDelta
                        val worldCentroid = toWorld(centroid)
                        presenter.zoomBy(factor, worldCentroid)
                    }
                }
            }

            // 4) Обработка колесика мыши и touchpad-scroll для изменения масштаба:
            //    Инвертируем логику (прокрутка внутрь = увеличение масштаба).
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            if (ev.type == PointerEventType.Scroll) {
                                val scroll = ev.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (scroll != 0f) {
                                    // Рассчитаем фактор зума с учётом текущей позиции ползунка
                                    val factor = if (scroll > 0f) 1f / (1f + scroll / (maxZoomSpeed - sliderPosition))
                                    else 1f + (-scroll / (maxZoomSpeed - sliderPosition))
                                    // Точка, относительно которой будет меняться масштаб
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

            // 5) Отслеживаем движение курсора для hover-эффекта:
            //    Если курсор над узлом, увеличиваем его.
            .pointerMoveFilter(onMove = { pos ->
                val world = toWorld(pos)
                hoverIndex = nodes.indexOfFirst {
                    (it.offset - world).getDistance() < it.radius
                }.takeIf { it != -1 }
                false
            }, onExit = {
                hoverIndex = null
                false
            })) {
            // --- РИСОВКА ВЕСОВ РЁБЕР (если включено) или стрелочных головок собираются в отдельный список ---
            val arrowHeads = mutableListOf<Pair<Path, Color>>()

            // 1) Рисуем все рёбра графа
            edges.forEachIndexed { i, list ->
                // Преобразуем координаты вершины «из мира в экран»
                val from = (nodes[i].offset - base) * z
                list.forEach { e ->
                    // Координата «до» (куда ведёт ребро)
                    val to = (nodes[e.vertex].offset - base) * z
                    // Линия для ребра: цвет задан в объекте Edge
                    drawLine(Color(e.color), from, to, strokeWidth = 2f * z)

                    // Если надо рисовать стрелки (ориентированные графы), сохраняем Path
                    if (showArrows && (presenter.graphType == GraphType.ORIENTED || presenter.graphType == GraphType.WEIGHTED_ORIENTED)) {
                        val angle = atan2(to.y - from.y, to.x - from.x)
                        val asz = 10f * z
                        val aang = (PI / 6).toFloat()
                        val p = Path().apply {
                            moveTo(to.x, to.y)
                            lineTo(to.x - asz * cos(angle - aang), to.y - asz * sin(angle - aang))
                            moveTo(to.x, to.y)
                            lineTo(to.x - asz * cos(angle + aang), to.y - asz * sin(angle + aang))
                        }
                        arrowHeads += p to Color(e.color)
                    }
                }
            }

            // 2) Рисуем узлы (вершины)
            nodes.forEachIndexed { idx, node ->
                // Если курсор наведен на узел, увеличиваем его в 1.2 раза
                val scale = if (idx == hoverIndex) 1.2f else 1f
                val radius = node.radius * z * scale
                // Выбранная вершина подсвечивается жёлтым цветом
                val color = if (idx == selectedIndex) Color.Yellow else Color(node.color)
                drawCircle(color, center = (node.offset - base) * z, radius = radius)
            }

            // 3) Отрисовываем стрелочные головки поверх линий ребер
            if (showArrows && (presenter.graphType == GraphType.ORIENTED || presenter.graphType == GraphType.WEIGHTED_ORIENTED)) {
                arrowHeads.forEach { (path, color) ->
                    drawPath(path, color, style = Stroke(width = 2f * z))
                }
            }
        }

        // --- OVERLAY: Text для весов рёбер (если нужно) ---
        if (showWeights && (presenter.graphType == GraphType.WEIGHTED_ORIENTED || presenter.graphType == GraphType.WEIGHTED_NON_ORIENTED)) {
            edges.forEachIndexed { i, list ->
                // Позиция начала ребра
                val from = (nodes[i].offset - base) * z
                list.forEach { e ->
                    val to = (nodes[e.vertex].offset - base) * z
                    // Средняя точка отрезка
                    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
                    // Переводим в dp для Text-компонента
                    val xDp = with(density) { mid.x.toDp() }
                    val yDp = with(density) { mid.y.toDp() }
                    Text(text = e.weight.toString(),
                        fontSize = (14 * z).sp,
                        color = Color(e.color),
                        modifier = Modifier.offset { IntOffset(xDp.roundToPx(), yDp.roundToPx()) })
                }
            }
        }

        // --- Отображаем текущий процент масштаба в правом нижнем углу ---
        Text(
            text = "Масштаб: ${(presenter.zoom * 100).toInt()}%",
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                .background(Color(0xAAFFFFFF), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 14.sp,
            color = Color.Black
        )

        // --- Ползунок для настройки скорости изменения масштаба (zoom) ---
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(text = "Скорость изменения масштаба: ${sliderPosition + 10f} %", fontSize = 15.sp)
            Slider(
                value = sliderPosition,
                valueRange = 0f..90f,
                steps = 9,
                onValueChange = { sliderPosition = it },
                modifier = Modifier.width(250.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFB71C1C),
                    activeTrackColor = Color(0xFFEF9A9A),
                    inactiveTrackColor = Color(0xFF888888),
                    inactiveTickColor = Color(0xFFFF0000),
                    activeTickColor = Color(0xFFB71C1C)
                )
            )
        }
    }
}

/**
 * Экран управления графом с тремя основными разделами:
 * 1) «Граф» – настройки отображения и цветовой схемы узлов;
 * 2) «Алгоритмы» – выбор и запуск алгоритмов на текущем графе;
 * 3) «Загрузка» – загрузка и переключение между сохранёнными графами из БД.
 *
 * Содержит верхнее меню с кнопками-переключателями для показа/скрытия соответствующих панелей.
 * В зависимости от выбранного раздела отображает:
 * - GraphPanel: переключатели для отображения стрелок и весов, выбор цвета новых узлов;
 * - AlgoPanel: список доступных алгоритмов для запуска через CanvasPresenter;
 * - LoadPanel: выпадающий список сохранённых графов, выбор хранилища (SQL, NEO4J, CSV), кнопки загрузки/обновления.
 *
 * @param presenter Объект CanvasPresenter, обеспечивающий взаимодействие с моделью графа и слоем данных.
 */
@Composable
fun GraphScreen(presenter: CanvasPresenter) {
    // Цвета для выбранных/невыбранных состояний кнопок меню
    val selectedBg = Color(0xFF7C1DFF)
    val unselectedBg = Color(0xFFF5F5F5)
    val selectedText = Color.White
    val unselectedText = Color.Black

    // Состояния для управления разворачиванием каждой панели
    var graphsExpanded by remember { mutableStateOf(false) }
    var algosExpanded by remember { mutableStateOf(false) }
    var loadExpanded by remember { mutableStateOf(false) }

    // Список доступных графов из БД и выбранный граф
    var graphList by remember { mutableStateOf<List<GraphMeta>>(emptyList()) }
    var selectedGraph by remember { mutableStateOf<GraphMeta?>(null) }

    // Управление выпадающим списком выбора хранилища
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("SQL") }
    val radioOptions = listOf("SQL", "NEO4J", "csv")

    // Текущий цвет для новых узлов и флаги отображения стрелок/весов
    var selectedColor by remember { mutableStateOf(Color.Blue) }
    var showArrows by remember { mutableStateOf(true) }
    var showWeights by remember { mutableStateOf(true) }

    // Загружаем список графов из SQL при первом отображении экрана
    LaunchedEffect(Unit) {
        graphList = GraphDbHelper.getAllGraphs()
    }

    Box(Modifier.fillMaxSize()) {
        // Основной фон и Canvas для отрисовки графа
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFEFEFEF)).zIndex(0f)
        ) {
            DraggableCanvasView(
                presenter = presenter,
                showArrows = showArrows,
                showWeights = showWeights,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Слой с панелями и меню поверх Canvas
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).zIndex(1f)
        ) {
            // Верхнее меню: три кнопки для переключения разделов
            Card(
                shape = RoundedCornerShape(8.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)
                ) {
                    // Кнопка «Граф»
                    Button(
                        onClick = {
                            graphsExpanded = !graphsExpanded
                            if (graphsExpanded) {
                                // Закрываем остальные панели
                                algosExpanded = false
                                loadExpanded = false
                            }
                        }, colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (graphsExpanded) selectedBg else unselectedBg,
                            contentColor = if (graphsExpanded) selectedText else unselectedText
                        )
                    ) {
                        Text("Граф")
                    }

                    // Кнопка «Алгоритмы»
                    Button(
                        onClick = {
                            algosExpanded = !algosExpanded
                            if (algosExpanded) {
                                graphsExpanded = false
                                loadExpanded = false
                            }
                        }, colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (algosExpanded) selectedBg else unselectedBg,
                            contentColor = if (algosExpanded) selectedText else unselectedText
                        )
                    ) {
                        Text("Алгоритмы")
                    }

                    // Кнопка «Загрузка»
                    Button(
                        onClick = {
                            loadExpanded = !loadExpanded
                            if (loadExpanded) {
                                graphsExpanded = false
                                algosExpanded = false
                            }
                        }, colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (loadExpanded) selectedBg else unselectedBg,
                            contentColor = if (loadExpanded) selectedText else unselectedText
                        )
                    ) {
                        Text("Загрузка")
                    }
                }
            }

            // Панель «Граф» разворачивается при graphsExpanded == true
            AnimatedVisibility(
                visible = graphsExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                GraphPanel(presenter = presenter,
                    showArrows = showArrows,
                    onToggleArrows = { showArrows = it },
                    showWeights = showWeights,
                    onToggleWeights = { showWeights = it },
                    selectedColor = selectedColor,
                    onSelectColor = { selectedColor = it })
            }

            // Панель «Алгоритмы» разворачивается при algosExpanded == true
            AnimatedVisibility(
                visible = algosExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                AlgoPanel(presenter = presenter)
            }

            // Панель «Загрузка» разворачивается при loadExpanded == true
            AnimatedVisibility(
                visible = loadExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                LoadPanel(graphList = graphList,
                    selectedGraph = selectedGraph,
                    expandedDropdown = expandedDropdown,
                    onToggleDropdown = { expandedDropdown = it },
                    onSelectGraph = { selectedGraph = it },
                    radioOptions = radioOptions,
                    selectedOption = selectedOption,
                    onOptionSelected = { selectedOption = it },
                    presenter = presenter,
                    onRefreshList = { graphList = GraphDbHelper.getAllGraphs() })
            }
        }
    }
}

/**
 * Панель настроек для управления графом:
 * - добавление и удаление вершин;
 * - изменение цвета и размера вершин;
 * - переключение отображения стрелок на ориентированных рёбрах;
 * - переключение отображения весов рёбер;
 * - выбор цвета для новых и существующих вершин.
 *
 * @param presenter Объект CanvasPresenter, через который выполняются все операции с моделью.
 * @param showArrows Текущий флаг для отображения стрелок на рёбрах. Если true, стрелки видимы.
 * @param onToggleArrows Лямбда, вызываемая при переключении чекбокса «Отображать стрелки».
 * @param showWeights Текущий флаг для отображения весов рёбер. Если true, веса отображаются.
 * @param onToggleWeights Лямбда, вызываемая при переключении чекбокса «Отображать веса».
 * @param selectedColor Текущий выбранный цвет для операций с вершинами.
 * @param onSelectColor Лямбда, вызывающаяся при выборе нового цвета из ColorDropdown.
 */
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
    Card(
        shape = RoundedCornerShape(8.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Первая строка: кнопки для работы с вершинами (добавление, покраска, удаление)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Добавить новую вершину выбранного цвета
                Button(onClick = { presenter.addCircle(selectedColor.toArgb()) }) {
                    Text("Добавить вершину")
                }
                // Закрасить все вершины выбранным цветом
                Button(onClick = { presenter.paintAll(selectedColor.toArgb()) }) {
                    Text("Окрасить все")
                }
                // Закрасить только выделенную вершину, если она выбрана
                Button(
                    onClick = { presenter.paintSelectedNode(selectedColor.toArgb()) },
                    enabled = presenter.selectedNodeIndex != null
                ) {
                    Text("Окрасить выбранную")
                }
                // Удалить выбранную вершину; кнопка активна только когда есть выбор
                Button(
                    onClick = { presenter.deleteSelectedNode() },
                    enabled = presenter.selectedNodeIndex != null,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Удалить выбранную", color = Color.White)
                }
            }

            // Вторая строка: кнопки для изменения размера выбранной вершины
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Увеличить радиус выбранной вершины на 5f
                Button(
                    onClick = { presenter.changeSelectedNodeRadius(5f) }, enabled = presenter.selectedNodeIndex != null
                ) {
                    Text("Увеличить размер")
                }
                // Уменьшить радиус выбранной вершины на 5f
                Button(
                    onClick = { presenter.changeSelectedNodeRadius(-5f) }, enabled = presenter.selectedNodeIndex != null
                ) {
                    Text("Уменьшить размер")
                }
            }

            // Третья строка: кнопки для масштабирования размеров всех вершин сразу
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Увеличить радиус всех вершин на 10%
                Button(onClick = { presenter.scaleAllNodeRadii(1.1f) }) {
                    Text("Увеличить все вершины")
                }
                // Уменьшить радиус всех вершин на 10%
                Button(onClick = { presenter.scaleAllNodeRadii(0.9f) }) {
                    Text("Уменьшить все вершины")
                }
            }

            // Четвёртая строка: чекбоксы для переключения отображения стрелок и весов
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = showArrows, onCheckedChange = onToggleArrows)
                Text("Отображать стрелки")
                Checkbox(checked = showWeights, onCheckedChange = onToggleWeights)
                Text("Отображать веса")
            }

            // Выпадающий список для выбора цвета новых и существующих вершин
            ColorDropdown(current = selectedColor, onSelect = onSelectColor)
        }
    }
}

/**
 * Панель управления алгоритмами для текущего графа.
 *
 * Содержит кнопки для запуска:
 * - ForceAtlas2 (с возможностью открыть настройки параметров);
 * - SCC (Strongly Connected Components) для ориентированных графов;
 * - MST (Minimum Spanning Tree) для взвешенных неориентированных графов;
 * - DLPA (Label Propagation) для поиска сообществ;
 * - Поиск циклов и Форд–Белман (Bellman-Ford) – оба требуют выбора вершины;
 * - Запоминание выбранной вершины для алгоритмов кратчайшего пути;
 * - Поиск мостов;
 * - Алгоритм Дейкстры – тоже требует выбранной вершины.
 *
 * При нажатии «Настройки FA2» открывается диалог с полями для редактирования
 * констант алгоритма ForceAtlas2: repulsion, attraction, damping, gravity, maxDisplacement.
 * После ввода новых значений вызывается метод Presenter.updateForceAtlasParams(...)
 * для пересоздания экземпляра ForceAtlas2Layout.
 *
 * @param presenter Объект CanvasPresenter, через который выполняются все операции и алгоритмы.
 */
@Composable
private fun AlgoPanel(presenter: CanvasPresenter) {
    // Флаг отображения окна с настройками ForceAtlas2
    var showFA2Settings by remember { mutableStateOf(false) }

    // Текущие параметры алгоритма ForceAtlas2 из Presenter
    val currentParams = remember {
        presenter.getForceAtlasParams()
    }

    // Локальные состояния для хранения текстовых значений полей ввода
    var repulsionText by remember { mutableStateOf(currentParams.repulsion.toString()) }
    var attractionText by remember { mutableStateOf(currentParams.attraction.toString()) }
    var dampingText by remember { mutableStateOf(currentParams.damping.toString()) }
    var gravityText by remember { mutableStateOf(currentParams.gravity.toString()) }
    var maxDispText by remember { mutableStateOf(currentParams.maxDisplacement.toString()) }

    Card(
        shape = RoundedCornerShape(8.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Первая строка кнопок: запуск ForceAtlas2, открытие настроек FA2, SCC, MST
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Запустить один шаг алгоритма ForceAtlas2 и обновить расположение узлов
                Button(onClick = { presenter.applyForceAtlas2Layout() }) {
                    Text("ForceAtlas2")
                }
                // Открыть диалог для изменения констант ForceAtlas2
                Button(onClick = { showFA2Settings = true }) {
                    Text("Настройки FA2")
                }
                // Запустить поиск сильно связанных компонент, только для ориентированного графа
                Button(
                    onClick = { presenter.highlightStronglyConnectedComponents() },
                    enabled = presenter.graphType == GraphType.ORIENTED
                ) {
                    Text("SCC")
                }
                // Построить минимальное остовное дерево, только для взвешенного неориентированного графа
                Button(
                    onClick = { presenter.highlightMinimumSpanningTree() },
                    enabled = presenter.graphType == GraphType.WEIGHTED_NON_ORIENTED
                ) {
                    Text("MST")
                }
            }

            // Вторая строка кнопок: DLPA, поиск циклов, Форд–Белман, запоминание вершины
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Label Propagation для поиска сообществ
                Button(onClick = { presenter.dlpa() }) {
                    Text("DLPA")
                }
                // Найти все простые циклы, содержащие выбранную вершину (если она выбрана)
                Button(
                    onClick = { presenter.searchCyles() }, enabled = presenter.selectedNodeIndex != null
                ) {
                    Text("Найти циклы")
                }
                // Алгоритм Форд–Белман для кратчайшего пути от запомненной до выбранной
                Button(
                    onClick = { presenter.FordBelman() }, enabled = presenter.selectedNodeIndex != null
                ) {
                    Text("Форд-Белман")
                }
                // Запомнить индекс выбранной вершины для последующих алгоритмов кратчайшего пути
                Button(
                    onClick = { presenter.MemorizeSelectedNode() }, enabled = presenter.selectedNodeIndex != null
                ) {
                    Text("Запомнить вершину")
                }
            }

            // Третья строка кнопок: поиск мостов и алгоритм Дейкстры
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Найти все мости в текущем неориентированном графе
                Button(onClick = { presenter.bridgeSearchAlg() }) {
                    Text("Поиск мостов")
                }
                // Запустить алгоритм Дейкстры для кратчайшего пути от запомненной до выбранной
                Button(
                    onClick = { presenter.DijkstraAlgorithm() }, enabled = presenter.selectedNodeIndex != null
                ) {
                    Text("Алгоритм Дейкстры")
                }
                Spacer(Modifier.width(16.dp))
            }
        }
    }

    // Диалог с настройками параметров ForceAtlas2
    if (showFA2Settings) {
        AlertDialog(onDismissRequest = { showFA2Settings = false },
            title = { Text(text = "Настройки ForceAtlas2") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Поле ввода для силы отталкивания (repulsion)
                    OutlinedTextField(
                        value = repulsionText,
                        onValueChange = { repulsionText = it },
                        label = { Text("Сила отталкивания (repulsion)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    // Поле ввода для силы притяжения (attraction)
                    OutlinedTextField(
                        value = attractionText,
                        onValueChange = { attractionText = it },
                        label = { Text("Сила притяжения (attraction)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    // Поле ввода для демпфирования (damping)
                    OutlinedTextField(
                        value = dampingText,
                        onValueChange = { dampingText = it },
                        label = { Text("Демпфирование (damping)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    // Поле ввода для гравитации (gravity)
                    OutlinedTextField(
                        value = gravityText,
                        onValueChange = { gravityText = it },
                        label = { Text("Гравитация (gravity)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    // Поле ввода для максимального смещения узла (maxDisplacement)
                    OutlinedTextField(
                        value = maxDispText,
                        onValueChange = { maxDispText = it },
                        label = { Text("Макс. смещение (maxDisplacement)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Пытаемся распарсить введённые значения в Double
                    val repulsionVal = repulsionText.toDoubleOrNull()
                    val attractionVal = attractionText.toDoubleOrNull()
                    val dampingVal = dampingText.toDoubleOrNull()
                    val gravityVal = gravityText.toDoubleOrNull()
                    val maxDispVal = maxDispText.toDoubleOrNull()

                    // Если хотя бы один параметр невалиден, просто возвращаемся и не закрываем диалог
                    if (repulsionVal == null || attractionVal == null || dampingVal == null || gravityVal == null || maxDispVal == null) {
                        return@Button
                    }

                    // Передаём новые параметры Presenter для пересоздания ForceAtlas2Layout
                    presenter.updateForceAtlasParams(
                        repulsion = repulsionVal,
                        attraction = attractionVal,
                        damping = dampingVal,
                        gravity = gravityVal,
                        maxDisplacement = maxDispVal
                    )

                    // Закрываем диалог
                    showFA2Settings = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(onClick = { showFA2Settings = false }) {
                    Text("Отмена")
                }
            })
    }
}

/**
 * Панель загрузки и сохранения графов.
 *
 * Позволяет выбрать источник данных (SQL, Neo4j или CSV),
 * загрузить выбранный граф, сохранить текущий граф в выбранное хранилище,
 * а также добавлять новый граф в SQL и удалять выбранный.
 *
 * @param graphList Список метаданных доступных графов из БД.
 * @param selectedGraph Текущий выбранный граф (или null, если не выбран).
 * @param expandedDropdown Флаг, показывающий состояние выпадающего списка выбора графа.
 * @param onToggleDropdown Лямбда для изменения видимости выпадающего списка.
 * @param onSelectGraph Лямбда, вызываемая при выборе графа из списка; принимает выбранный GraphMeta или null.
 * @param radioOptions Список вариантов источников данных (например, "SQL", "NEO4J", "csv").
 * @param selectedOption Текущий выбранный вариант источника данных.
 * @param onOptionSelected Лямбда, вызываемая при выборе нового варианта из radioOptions.
 * @param presenter Объект CanvasPresenter для выполнения операций загрузки/сохранения графа.
 * @param onRefreshList Лямбда для обновления списка graphList после операций (сохранение как новый или удаление).
 */
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
    Card(
        shape = RoundedCornerShape(8.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок секции с переключателями источника данных
            Text("Источник данных:")
            // Перебираем все radioOptions и рисуем кнопку для каждого
            radioOptions.forEach { opt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (opt == selectedOption), onClick = { onOptionSelected(opt) })
                    Text(opt)
                }
            }

            // Кнопки для операций: Сохранить, Загрузить, Сохранить как новый, Удалить
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Сохранение в выбранное хранилище
                Button(
                    onClick = {
                        when (selectedOption) {
                            "NEO4J" -> selectedGraph?.let { presenter.saveNeo4jGraph(it.id) }
                            "csv" -> presenter.saveCSVGraph()
                            else -> selectedGraph?.let { presenter.saveGraph(it.id) }
                        }
                    },
                    // Доступна, если выбран граф или выбран CSV (который не требует выбора)
                    enabled = (selectedGraph != null || selectedOption == "csv")
                ) {
                    Text("Сохранить")
                }

                // Загрузка из выбранного хранилища
                Button(
                    onClick = {
                        when (selectedOption) {
                            "NEO4J" -> selectedGraph?.let { presenter.loadNeo4jGraph(it.id) }
                            "csv" -> presenter.loadCSVGraph()
                            else -> selectedGraph?.let { presenter.loadGraph(it.id) }
                        }
                    }, enabled = (selectedGraph != null || selectedOption == "csv")
                ) {
                    Text("Загрузить")
                }

                // Сохранить текущий граф как новый в SQL: получаем новый ID, сохраняем и обновляем список
                Button(onClick = {
                    val newId = GraphDbHelper.getNextGraphId()
                    presenter.saveGraph(newId)
                    onRefreshList()
                    // После сохранения автоматически выбираем только что созданный граф
                    GraphDbHelper.getAllGraphs().firstOrNull { it.id == newId }?.let { onSelectGraph(it) }
                }) {
                    Text("Сохранить как новый")
                }

                // Удаление выбранного графа из SQL; кнопка активна, только если выбран граф
                Button(
                    onClick = {
                        selectedGraph?.let {
                            GraphDbHelper.deleteGraph(it.id)
                            onRefreshList()
                            onSelectGraph(null)
                        }
                    },
                    enabled = (selectedGraph != null),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Удалить", color = Color.White)
                }
            }

            // Блок с выпадающим списком для выбора существующего графа
            Box {
                Button(onClick = { onToggleDropdown(true) }) {
                    Text(selectedGraph?.let { "Граф #${it.id}" } ?: "Выбрать граф")
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown, contentDescription = "Открыть список"
                    )
                }
                DropdownMenu(expanded = expandedDropdown, onDismissRequest = { onToggleDropdown(false) }) {
                    graphList.forEach { meta ->
                        DropdownMenuItem(onClick = {
                            onSelectGraph(meta)
                            onToggleDropdown(false)
                        }) {
                            Text("Граф #${meta.id} (${meta.type})")
                        }
                    }
                }
            }
        }
    }
}


/**
 * Выпадающий список для выбора цвета.
 *
 * Отображает текущий выбранный цвет как текстовую метку,
 * а при нажатии показывает меню из предопределённых цветов.
 * После выбора нового варианта вызывает onSelect с новым Color.
 *
 * @param current Текущий выбранный цвет. Используется для вывода метки на кнопке.
 * @param onSelect Лямбда, вызываемая при выборе нового цвета; принимает выбранный Color.
 */
@Composable
fun ColorDropdown(
    current: Color, onSelect: (Color) -> Unit
) {
    // Флаг, раскрыто ли меню
    var expanded by remember { mutableStateOf(false) }

    // Список доступных пар (цвет, название)
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
        // Кнопка, выводящая название текущего выбранного цвета
        Button(onClick = { expanded = true }) {
            Text(options.first { it.first == current }.second)
        }

        // Сам список меню с вариантами
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
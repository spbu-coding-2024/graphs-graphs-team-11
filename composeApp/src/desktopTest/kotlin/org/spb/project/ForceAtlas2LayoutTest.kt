package org.spb.project

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.spb.project.common.Graph
import org.spb.project.common.GraphType
import org.spb.project.presenter.ForceAtlas2Layout
import org.spb.project.presenter.MinimumSpanningTree
import kotlin.math.round
import kotlin.math.sqrt

class ForceAtlas2LayoutTest {

    private val blackColor = 0xFF000000.toInt()

    @Test
    fun testNoMovementWhenAllConstantsZero() {
        val graph = Graph(GraphType.NON_ORIENTED)
        graph.addVertex(1.0, 2.0)
        graph.addVertex(3.0, 4.0)
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = Double.MAX_VALUE
        )
        val before = graph.getVertexes().map { it.x to it.y }
        layout.applyLayout(graph)
        val after = graph.getVertexes().map { it.x to it.y }
        assertEquals(before, after)
    }

    @Test
    fun testRepulsionBetweenTwoVertices() {
        val graph = Graph(GraphType.NON_ORIENTED)
        graph.addVertex(0.0, 0.0)
        graph.addVertex(1.0, 0.0)
        val layout = ForceAtlas2Layout(
            repulsionConstant = 100.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = Double.MAX_VALUE
        )
        layout.applyLayout(graph)
        val v1 = graph.getVertexes()[0]
        val v2 = graph.getVertexes()[1]
        assertTrue(v1.x < 0.0, "Vertex1 should move left under repulsion")
        assertTrue(v2.x > 1.0, "Vertex2 should move right under repulsion")
    }

    @Test
    fun testAttractionAlongEdge() {
        val graph = Graph(GraphType.NON_ORIENTED)
        graph.addVertex(0.0, 0.0)
        graph.addVertex(10.0, 0.0)
        graph.addEdge(startVertex = 0, finalVertex = 1, weight = 1, color = 0)
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 1.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = Double.MAX_VALUE
        )
        layout.applyLayout(graph)
        val v1 = graph.getVertexes()[0]
        val v2 = graph.getVertexes()[1]
        assertTrue(v1.x > 0.0, "Vertex1 should move right under attraction")
        assertTrue(v2.x < 10.0, "Vertex2 should move left under attraction")
    }

    @Test
    fun testGravityPullsToCenter() {
        val graph = Graph(GraphType.NON_ORIENTED)
        graph.addVertex(5.0, 0.0)
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 1.0,
            maxDisplacement = Double.MAX_VALUE
        )
        layout.applyLayout(graph)
        val v = graph.getVertexes()[0]
        assertTrue(v.x < 5.0, "Vertex should be pulled toward center by gravity")
    }

    @Test
    fun testGravitySymmetryThreeVertices() {
        val graph = Graph(GraphType.NON_ORIENTED)
        graph.addVertex(3.0, 4.0)
        graph.addVertex(-3.0, 4.0)
        graph.addVertex(0.0, -5.0)
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 1.0,
            maxDisplacement = Double.MAX_VALUE
        )
        layout.applyLayout(graph)
        val distances = graph.getVertexes().map { v -> sqrt(v.x * v.x + v.y * v.y) }
        distances.forEach { distance ->
            assertEquals(4.0, round(distance), 1e-6, "All vertices should move exactly 1 unit toward origin")
        }
    }

    @Test
    fun testDampingEffect() {
        val initial = listOf(-1.0 to 0.0, 1.0 to 0.0)
        // No damping
        val graphNoDamp = Graph(GraphType.NON_ORIENTED)
        initial.forEach { graphNoDamp.addVertex(it.first, it.second) }
        val layoutNoDamp = ForceAtlas2Layout(
            repulsionConstant = 100.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = Double.MAX_VALUE
        )
        layoutNoDamp.applyLayout(graphNoDamp)
        val dxNoDamp = graphNoDamp.getVertexes()[0].x - initial[0].first

        // With damping = 0.5
        val graphHalfDamp = Graph(GraphType.NON_ORIENTED)
        initial.forEach { graphHalfDamp.addVertex(it.first, it.second) }
        val layoutHalfDamp = ForceAtlas2Layout(
            repulsionConstant = 100.0,
            attractionConstant = 0.0,
            damping = 0.5,
            gravity = 0.0,
            maxDisplacement = Double.MAX_VALUE
        )
        layoutHalfDamp.applyLayout(graphHalfDamp)
        val dxHalfDamp = graphHalfDamp.getVertexes()[0].x - initial[0].first

        assertEquals(dxNoDamp * 0.5, dxHalfDamp, 1e-6, "Displacement should scale with damping factor")
    }

    @Test
    fun testMaxDisplacementCap() {
        val graph = Graph(GraphType.NON_ORIENTED)
        graph.addVertex(100.0, 0.0)
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 10.0,
            maxDisplacement = 1.0
        )
        layout.applyLayout(graph)
        val v = graph.getVertexes()[0]
        // Gravity would pull by 10 units, but cap is 1
        assertEquals(99.0, v.x, 1e-6, "Displacement should be capped by maxDisplacement")
    }

    @Test
    fun testEdgeWeightIgnored() {
        // Create two graphs differing only by edge weight
        val graphLowWeight = Graph(GraphType.NON_ORIENTED)
        graphLowWeight.addVertex(0.0, 0.0)
        graphLowWeight.addVertex(10.0, 0.0)
        graphLowWeight.addEdge(0, 1, weight = 1, color = 0)

        val graphHighWeight = Graph(GraphType.NON_ORIENTED)
        graphHighWeight.addVertex(0.0, 0.0)
        graphHighWeight.addVertex(10.0, 0.0)
        graphHighWeight.addEdge(0, 1, weight = 1000, color = 0)

        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 1.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = Double.MAX_VALUE
        )
        layout.applyLayout(graphLowWeight)
        layout.applyLayout(graphHighWeight)

        val dxLow = graphLowWeight.getVertexes()[0].x
        val dxHigh = graphHighWeight.getVertexes()[0].x
        assertEquals(dxLow, dxHigh, 1e-6, "Edge weight should not affect attraction calculation")
    }

    @Test
    fun parallelEdgesChooseLowestWeight() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(0.0, 0.0)
        graph.addVertex(1.0, 1.0)
        // две параллельные дуги разного веса
        graph.addEdge(0, 1, 7, blackColor); graph.addEdge(1, 0, 7, blackColor)
        graph.addEdge(0, 1, 3, blackColor); graph.addEdge(1, 0, 3, blackColor)

        val mst = MinimumSpanningTree().buildMST(graph)
        // должен быть выбран только наименьший из параллельных ребёр
        assertEquals(1, mst.size, "Должно быть ровно одно ребро в MST")
        assertEquals(
            MinimumSpanningTree.MSTEdge(0, 1, 3),
            mst[0],
            "Должно быть выбрано ребро весом 3, а не 7"
        )
    }

    @Test
    fun negativeWeightsHandledCorrectly() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(3) { graph.addVertex(it.toDouble(), it.toDouble()) }
        // один отрицательный, один положительный, одно альтернативное
        graph.addEdge(0, 1, -1, blackColor); graph.addEdge(1, 0, -1, blackColor)
        graph.addEdge(1, 2, 5, blackColor); graph.addEdge(2, 1, 5, blackColor)
        graph.addEdge(0, 2, 2, blackColor); graph.addEdge(2, 0, 2, blackColor)

        val mst = MinimumSpanningTree().buildMST(graph)
        // должны быть выбраны ребра весом -1 и 2
        val weights = mst.map { it.weight }.sorted()
        assertEquals(
            listOf(-1, 2),
            weights,
            "MST должен включать как отрицательное ребро, так и минимальное из положительных"
        )
    }

    @Test
    fun alreadyTreeGraphReturnsSameEdges() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(4) { graph.addVertex(it.toDouble(), it.toDouble()) }
        // граф уже является деревом: 0–1–2–3
        graph.addEdge(0, 1, 1, blackColor); graph.addEdge(1, 0, 1, blackColor)
        graph.addEdge(1, 2, 2, blackColor); graph.addEdge(2, 1, 2, blackColor)
        graph.addEdge(2, 3, 3, blackColor); graph.addEdge(3, 2, 3, blackColor)

        val mst = MinimumSpanningTree().buildMST(graph)
        val expected = listOf(
            MinimumSpanningTree.MSTEdge(0, 1, 1),
            MinimumSpanningTree.MSTEdge(1, 2, 2),
            MinimumSpanningTree.MSTEdge(2, 3, 3)
        )
        // должен вернуть точно те же рёбра, что и исходный «деревянный» граф
        assertEquals(expected, mst, "Если граф уже дерево, MST должен совпадать с исходными рёбрами")
    }

    @Test
    fun zeroWeightEdgesHandledCorrectly() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(3) { graph.addVertex(it.toDouble(), 0.0) }
        // два нулевых ребра и одно тяжёлое
        graph.addEdge(0, 1, 0, blackColor); graph.addEdge(1, 0, 0, blackColor)
        graph.addEdge(1, 2, 0, blackColor); graph.addEdge(2, 1, 0, blackColor)
        graph.addEdge(0, 2, 5, blackColor); graph.addEdge(2, 0, 5, blackColor)

        val mst = MinimumSpanningTree().buildMST(graph)
        // должны быть выбраны именно оба нулевых ребра
        val weights = mst.map { it.weight }
        assertEquals(2, mst.size, "MST должен содержать ровно два ребра")
        assertTrue(weights.all { it == 0 }, "Все выбранные рёбра должны иметь нулевой вес")
    }

    @Test
    fun emptyGraphShouldNotFail() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        // пустой граф — никаких вершин, applyLayout должен отработать без ошибок
        ForceAtlas2Layout().applyLayout(graph)
        assertTrue(graph.getVertexes().isEmpty(), "Пустой граф остаётся пустым после applyLayout")
    }

    @Test
    fun singleVertexGravityPullsToCenter() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(10.0, 0.0)  // изначально далеко от центра
        // больше нет отталкивания и притяжения по рёбрам, только гравитация и без демпфирования
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 1.0,
            maxDisplacement = 100.0
        )
        layout.applyLayout(graph)

        val v = graph.getVertexes()[0]
        // должна приблизиться к (0,0)
        assertTrue(v.x < 10.0, "X координата должна уменьшиться под действием гравитации")
        assertEquals(0.0, v.y, "Y координата остаётся на нуле")
    }

    @Test
    fun repulsionSeparatesVertices() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(-1.0, 0.0)
        graph.addVertex( 1.0, 0.0)
        // только отталкивание
        val layout = ForceAtlas2Layout(
            repulsionConstant = 100.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = 1000.0
        )
        val before = distance(graph.getVertexes()[0], graph.getVertexes()[1])
        layout.applyLayout(graph)
        val after  = distance(graph.getVertexes()[0], graph.getVertexes()[1])
        assertTrue(after > before, "Расстояние между вершинами должно вырасти из-за отталкивания")
    }

    @Test
    fun attractionBringsConnectedVerticesCloser() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(-2.0, 0.0)
        graph.addVertex( 2.0, 0.0)
        // создаём «ребро» в обе стороны для притяжения
        graph.addEdge(0, 1, weight = 0, color = 0)
        graph.addEdge(1, 0, weight = 0, color = 0)
        // только притяжение, без отталкивания и гравитации
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 0.5,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = 1000.0
        )
        val before = distance(graph.getVertexes()[0], graph.getVertexes()[1])
        layout.applyLayout(graph)
        val after  = distance(graph.getVertexes()[0], graph.getVertexes()[1])
        assertTrue(after == before, "Расстояние между связанными вершинами должно уменьшиться из-за притяжения")
    }

    @Test
    fun maxDisplacementLimitsMovement() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(0.0, 0.0)
        graph.addVertex(0.01, 0.0)
        // очень сильное отталкивание, но малое maxDisplacement
        val layout = ForceAtlas2Layout(
            repulsionConstant = 1e6,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = 1.0
        )
        // сохраняем старые координаты
        val oldPositions = graph.getVertexes().map { it.x to it.y }
        layout.applyLayout(graph)

        graph.getVertexes().forEachIndexed { i, v ->
            val (oldX, oldY) = oldPositions[i]
            val dx = v.x - oldX
            val dy = v.y - oldY
            val move = sqrt(dx*dx + dy*dy)
            assertTrue(move <= 1.0 + 1e-6, "Смещение вершины не должно превышать maxDisplacement")
        }
    }

    @Test
    fun noForcesResultsInNoMovement() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 2.0)
        graph.addVertex(-3.0, 4.0)
        // все константы = 0, applyLayout не должен менять позиции
        val layout = ForceAtlas2Layout(
            repulsionConstant = 0.0,
            attractionConstant = 0.0,
            damping = 1.0,
            gravity = 0.0,
            maxDisplacement = 10.0
        )
        val before = graph.getVertexes().map { it.x to it.y }
        layout.applyLayout(graph)
        val after  = graph.getVertexes().map { it.x to it.y }
        assertEquals(before, after, "Без сил вершины должны оставаться на месте")
    }

    // вспомогательная функция для вычисления евклидова расстояния
    private fun distance(v1: org.spb.project.common.Vertex, v2: org.spb.project.common.Vertex): Double {
        val dx = v1.x - v2.x
        val dy = v1.y - v2.y
        return sqrt(dx*dx + dy*dy)
    }
}
package org.spb.project.presenter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spb.project.common.Graph
import org.spb.project.common.GraphType

class MinimumSpanningTreeTest {

    private val blackColor = 0xFF000000.toInt()

    @Test
    fun emptyGraphReturnsEmptyMST() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        val mst = MinimumSpanningTree().buildMST(graph)
        assertTrue(mst.isEmpty(), "MST пустого графа должен быть пустым")
    }

    @Test
    fun singleVertexGraphReturnsEmptyMST() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(0.0, 0.0)
        val mst = MinimumSpanningTree().buildMST(graph)
        assertTrue(mst.isEmpty(), "MST графа с одной вершиной должен быть пустым")
    }

    @Test
    fun twoConnectedVerticesReturnSingleEdge() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(0.0, 0.0)
        graph.addVertex(1.0, 1.0)
        graph.addEdge(0, 1, 5, blackColor)
        graph.addEdge(1, 0, 5, blackColor)
        val mst = MinimumSpanningTree().buildMST(graph)
        assertEquals(1, mst.size, "MST должен содержать ровно одно ребро")
        assertEquals(
            MinimumSpanningTree.MSTEdge(0, 1, 5),
            mst[0],
            "Единственное ребро в MST должно быть (0–1) весом 5"
        )
    }

    @Test
    fun triangleGraphSelectsTwoMinimalEdges() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(3) { graph.addVertex(it.toDouble(), it.toDouble()) }
        graph.addEdge(0, 1, 1, blackColor); graph.addEdge(1, 0, 1, blackColor)
        graph.addEdge(0, 2, 3, blackColor); graph.addEdge(2, 0, 3, blackColor)
        graph.addEdge(1, 2, 2, blackColor); graph.addEdge(2, 1, 2, blackColor)
        val mst = MinimumSpanningTree().buildMST(graph)
        val expected = listOf(
            MinimumSpanningTree.MSTEdge(0, 1, 1),
            MinimumSpanningTree.MSTEdge(1, 2, 2)
        )
        assertEquals(expected, mst, "Для треугольного графа MST должен выбирать два минимальных ребра")
    }

    @Test
    fun disconnectedGraphReturnsForest() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(4) { graph.addVertex(it.toDouble(), 0.0) }
        graph.addEdge(0, 1, 4, blackColor); graph.addEdge(1, 0, 4, blackColor)
        graph.addEdge(2, 3, 2, blackColor); graph.addEdge(3, 2, 2, blackColor)
        val mst = MinimumSpanningTree().buildMST(graph)
        val expected = listOf(
            MinimumSpanningTree.MSTEdge(2, 3, 2),
            MinimumSpanningTree.MSTEdge(0, 1, 4)
        )
        assertEquals(expected, mst, "Несвязный граф должен давать лес минимального остова для каждого компонента")
    }

    @Test
    fun equalWeightEdgesAreHandledWithoutDuplicates() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(4) { graph.addVertex(it.toDouble(), it.toDouble()) }
        // квадрат со всеми рёбрами весом 5
        graph.addEdge(0, 1, 5, blackColor); graph.addEdge(1, 0, 5, blackColor)
        graph.addEdge(1, 2, 5, blackColor); graph.addEdge(2, 1, 5, blackColor)
        graph.addEdge(2, 3, 5, blackColor); graph.addEdge(3, 2, 5, blackColor)
        graph.addEdge(3, 0, 5, blackColor); graph.addEdge(0, 3, 5, blackColor)

        val mst = MinimumSpanningTree().buildMST(graph)
        assertEquals(3, mst.size, "MST должен содержать ровно n-1 ребро для n=4")
        assertEquals(mst.toSet().size, mst.size, "Все выбранные ребра должны быть уникальными")
    }

    @Test
    fun cycleGraphDoesNotIncludeCycle() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(3) { graph.addVertex(it.toDouble(), 0.0) }
        // создаём цикл 0–1–2–0 с разными весами
        graph.addEdge(0, 1, 2, blackColor); graph.addEdge(1, 0, 2, blackColor)
        graph.addEdge(1, 2, 2, blackColor); graph.addEdge(2, 1, 2, blackColor)
        graph.addEdge(2, 0, 1, blackColor); graph.addEdge(0, 2, 1, blackColor)

        val mst = MinimumSpanningTree().buildMST(graph)
        // должны быть выбраны два наименьших ребра: (2–0) вес 1 и одно из (0–1) или (1–2) весом 2
        assertTrue(mst.any { it == MinimumSpanningTree.MSTEdge(0, 2, 1) }, "MST должен содержать ребро (0–2) весом 1")
        assertEquals(2, mst.size, "MST должен содержать ровно два ребра")
    }

    @Test
    fun largerGraphGeneratesCorrectMST() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        repeat(5) { graph.addVertex(it.toDouble(), it.toDouble()) }
        var weight = 1
        for (u in 0 until 5) for (v in u + 1 until 5) {
            graph.addEdge(u, v, weight, blackColor)
            graph.addEdge(v, u, weight, blackColor)
            weight++
        }
        val mst = MinimumSpanningTree().buildMST(graph)
        val weights = mst.map { it.weight }.sorted()
        assertEquals(listOf(1, 2, 3, 4), weights, "MST полного графа из 5 вершин должен содержать веса 1, 2, 3 и 4")
        assertEquals(4, mst.size, "MST должен иметь ровно n-1=4 ребра")
    }
}

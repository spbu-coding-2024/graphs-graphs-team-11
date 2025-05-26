package org.spb.project.presenter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spb.project.common.Graph
import org.spb.project.common.GraphType

class StronglyConnectedComponentsTest {

    private val dummyWeight = 0
    private val dummyColor = 0

    @Test
    fun emptyGraphReturnsNoComponents() {
        // пустой граф не содержит вершин — возвращается пустой список компонент
        val graph = Graph(GraphType.ORIENTED)
        val scc = StronglyConnectedComponents().findComponents(graph)
        assertTrue(scc.isEmpty(), "Пустой граф должен возвращать пустой список компонент")
    }

    @Test
    fun singleVertexGraphReturnsSingleComponent() {
        // граф из одной вершины без рёбер — одна компонента [0]
        val graph = Graph(GraphType.ORIENTED)
        graph.addVertex(0.0, 0.0)
        val scc = StronglyConnectedComponents().findComponents(graph)
        assertEquals(1, scc.size, "Граф с одной вершиной должен давать одну компоненту")
        assertEquals(listOf(listOf(0)), scc, "Единственная компонента должна содержать вершину 0")
    }

    @Test
    fun graphWithNoEdgesReturnsSingleVertexComponents() {
        // три изолированные вершины без рёбер — каждая в своей компоненте
        val graph = Graph(GraphType.ORIENTED)
        repeat(3) { graph.addVertex(it.toDouble(), it.toDouble()) }
        val scc = StronglyConnectedComponents().findComponents(graph)
        assertEquals(3, scc.size, "Граф без рёбер должен давать по одной компоненте на вершину")
        // проверяем наличие компонент {0}, {1}, {2} в любом порядке
        val comps = scc.map { it.toSet() }.toSet()
        assertEquals(setOf(setOf(0), setOf(1), setOf(2)), comps)
    }

    @Test
    fun mutualConnectionFormsOneComponent() {
        // две вершины с двунаправленным ребром — одна сильносвязная компонента
        val graph = Graph(GraphType.ORIENTED)
        graph.addVertex(0.0, 0.0)
        graph.addVertex(1.0, 1.0)
        graph.addEdge(0, 1, dummyWeight, dummyColor)
        graph.addEdge(1, 0, dummyWeight, dummyColor)
        val scc = StronglyConnectedComponents().findComponents(graph)
        assertEquals(1, scc.size, "Взаимосвязанные вершины должны попасть в одну компоненту")
        assertEquals(setOf(0, 1), scc[0].toSet(), "Компонента должна содержать вершины 0 и 1")
    }

    @Test
    fun selfLoopDoesNotAffectComponent() {
        // петля на вершине — не влияет на состав компоненты
        val graph = Graph(GraphType.ORIENTED)
        graph.addVertex(0.0, 0.0)
        graph.addEdge(0, 0, dummyWeight, dummyColor)
        val scc = StronglyConnectedComponents().findComponents(graph)
        assertEquals(1, scc.size, "Граф с одной вершиной и петлёй должен давать одну компоненту")
        assertEquals(listOf(listOf(0)), scc, "Компонента должна содержать только вершину 0")
    }

    @Test
    fun linearGraphReturnsEachVertexAlone() {
        // ориентированный путь 0→1→2→3 без обратных рёбер
        val graph = Graph(GraphType.ORIENTED)
        repeat(4) { graph.addVertex(it.toDouble(), 0.0) }
        graph.addEdge(0, 1, dummyWeight, dummyColor)
        graph.addEdge(1, 2, dummyWeight, dummyColor)
        graph.addEdge(2, 3, dummyWeight, dummyColor)
        val scc = StronglyConnectedComponents().findComponents(graph)
        assertEquals(4, scc.size, "В ациклическом ориентированном графе каждая вершина — отдельная компонента")
        val comps = scc.map { it.toSet() }.toSet()
        assertEquals(setOf(setOf(0), setOf(1), setOf(2), setOf(3)), comps)
    }

    @Test
    fun complexGraphReturnsCorrectComponents() {
        // граф с одним циклом 0→1→2→0 и двумя одиночными вершинами 3→4 (но без 4→3)
        val graph = Graph(GraphType.ORIENTED)
        repeat(5) { graph.addVertex(it.toDouble(), it.toDouble()) }
        // цикл между 0,1,2
        graph.addEdge(0, 1, dummyWeight, dummyColor)
        graph.addEdge(1, 2, dummyWeight, dummyColor)
        graph.addEdge(2, 0, dummyWeight, dummyColor)
        // одностороннее ребро 3→4
        graph.addEdge(3, 4, dummyWeight, dummyColor)
        val scc = StronglyConnectedComponents().findComponents(graph)
        // ожидаем 3 компоненты: {0,1,2}, {3}, {4}
        val expected = setOf(
            setOf(0, 1, 2),
            setOf(3),
            setOf(4)
        )
        val actual = scc.map { it.toSet() }.toSet()
        assertEquals(expected, actual, "Должны быть найдены компоненты {0,1,2}, {3} и {4}")
    }
}

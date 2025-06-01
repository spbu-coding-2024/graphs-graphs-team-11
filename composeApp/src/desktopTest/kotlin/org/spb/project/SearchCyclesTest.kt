package org.spb.project

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType
import org.spb.project.presenter.algorithm.SearchCycles

class SearchCyclesTest {

    @Test
    fun `OneVertexGraphReturnsEmptySC`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        var cycles = SearchCycles(graph,0).search()
        assertTrue(cycles.isEmpty())
    }

    @Test
    fun `OneCycleGraphReturnsCycleSC`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,1,1)
        graph.addEdge(1,2,1,1)
        graph.addEdge(2,0,1,1)
        var cycles = SearchCycles(graph,0).search()
        assertEquals(1, cycles.size)
        assertEquals(3, cycles[0].size)
        assertEquals(1, cycles[0][0])
        assertEquals(2, cycles[0][1])
        assertEquals(0, cycles[0][2])
    }

    @Test
    fun `TwoCycleGraphReturns2CycleSC`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,1,1)
        graph.addEdge(1,2,1,1)
        graph.addEdge(2,0,1,1)
        graph.addEdge(0,3,1,1)
        graph.addEdge(3,4,1,1)
        graph.addEdge(4,0,1,1)
        var cycles = SearchCycles(graph,0).search()
        assertEquals(2, cycles.size)
        assertEquals(3, cycles[0].size)
        assertEquals(3, cycles[0].size)
        assertEquals(1, cycles[0][0])
        assertEquals(2, cycles[0][1])
        assertEquals(0, cycles[0][2])
        assertEquals(3, cycles[1][0])
        assertEquals(4, cycles[1][1])
        assertEquals(0, cycles[0][2])
    }

    @Test
    fun `TwoCycleGraphReturns1CycleSC`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,1,1)
        graph.addEdge(1,2,1,1)
        graph.addEdge(2,0,1,1)
        graph.addEdge(0,3,1,1)
        graph.addEdge(3,4,1,1)
        graph.addEdge(4,0,1,1)
        var cycles = SearchCycles(graph,3).search()
        assertEquals(1, cycles.size)
        assertEquals(3, cycles[0].size)
        assertEquals(4, cycles[0][0])
        assertEquals(0, cycles[0][1])
        assertEquals(3, cycles[0][2])
    }

    @Test
    fun `TwoConnectedCycleGraphReturns2CycleSC`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,1,1)
        graph.addEdge(1,2,1,1)
        graph.addEdge(2,3,1,1)
        graph.addEdge(3,0,1,1)
        graph.addEdge(1,4,1,1)
        graph.addEdge(4,5,1,1)
        graph.addEdge(5,3,1,1)
        var cycles = SearchCycles(graph,0).search()
        assertEquals(2, cycles.size)
        assertEquals(4, cycles[0].size)
        assertEquals(5, cycles[1].size)
        assertEquals(1, cycles[0][0])
        assertEquals(2, cycles[0][1])
        assertEquals(3, cycles[0][2])
        assertEquals(0, cycles[0][3])
        assertEquals(1, cycles[1][0])
        assertEquals(4, cycles[1][1])
        assertEquals(5, cycles[1][2])
        assertEquals(3, cycles[1][3])
        assertEquals(0, cycles[1][4])
    }

}

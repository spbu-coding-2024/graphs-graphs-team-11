package org.spb.project

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spb.project.common.Graph
import org.spb.project.common.GraphType
import org.spb.project.presenter.FordBelmanShortPath

class FordBelmanTestsTest {

    var inf = 2147483647
    @Test
    fun `OneVertexGraphFB`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        var path = FordBelmanShortPath(graph, 0, 0).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(1, path.size)
        assertEquals(0, path[0])
    }

    @Test
    fun `TwovertexGraphFB`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,1,1)
        var path = FordBelmanShortPath(graph, 0, 1).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(1, FordBelmanShortPath(graph, 0, 1).fordBelman())
        assertEquals(2, path.size)
        assertEquals(0, path[0])
        assertEquals(1, path[1])
    }

    @Test
    fun `ThreeVertexGraphFB`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,1,1)
        graph.addEdge(1,2,1,1)
        var path = FordBelmanShortPath(graph, 0, 2).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(2, FordBelmanShortPath(graph, 0, 2).fordBelman())
        assertEquals(3, path.size)
        assertEquals(0, path[0])
        assertEquals(1, path[1])
        assertEquals(2, path[2])
    }


    @Test
    fun `NegativeCycleGraphFB`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,-1,1)
        graph.addEdge(1,2,-1,1)
        graph.addEdge(2,0,-1,1)
        graph.addEdge(2,3,-1,1)
        var path = FordBelmanShortPath(graph, 0, 3).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(-inf, FordBelmanShortPath(graph, 0, 3).fordBelman())
        assertEquals(0, path.size)
    }

    @Test
    fun `NegativeCycleGraph2FB`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,-1,1)
        graph.addEdge(1,2,-2,1)
        graph.addEdge(2,0,-3,1)
        graph.addEdge(2,3,-5,1)
        var path = FordBelmanShortPath(graph, 0, 3).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(-inf, FordBelmanShortPath(graph, 0, 3).fordBelman())
        assertEquals(0, path.size)
    }
    @Test
    fun `NegativeCycleGraph3FB`() {
        val graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0,1,-1,1)
        graph.addEdge(1,2,-2,1)
        graph.addEdge(2,0,-3,1)
        graph.addEdge(2,3,-5,1)
        graph.addEdge(4,0,1,1)
        var path = FordBelmanShortPath(graph, 4, 3).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(-inf, FordBelmanShortPath(graph, 4, 3).fordBelman())
        assertEquals(0, path.size)
    }
    @Test
    fun `TwopathsGraph3FB`() {
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
        graph.addEdge(3,4,1,1)
        graph.addEdge(0,5,1,1)
        graph.addEdge(5,6,1,1)
        graph.addEdge(6,4,1,1)
        var path = FordBelmanShortPath(graph, 0, 4).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(3, FordBelmanShortPath(graph, 0, 4).fordBelman())
        assertEquals(4, path.size)
        assertEquals(0, path[0])
        assertEquals(5, path[1])
        assertEquals(6, path[2])
        assertEquals(4, path[3])

    }
    @Test
    fun `TwopathsOneFakeGraph3FB`() {
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
        graph.addEdge(3,4,1,1)
        graph.addEdge(0,5,1,1)
        graph.addEdge(5,6,5,1)
        graph.addEdge(6,4,1,1)
        var path = FordBelmanShortPath(graph, 0, 4).getShortestPath() ?: mutableListOf<Int>()
        assertEquals(4, FordBelmanShortPath(graph, 0, 4).fordBelman())
        assertEquals(5, path.size)
        assertEquals(0, path[0])
        assertEquals(1, path[1])
        assertEquals(2, path[2])
        assertEquals(3, path[3])
        assertEquals(4, path[4])

    }

}
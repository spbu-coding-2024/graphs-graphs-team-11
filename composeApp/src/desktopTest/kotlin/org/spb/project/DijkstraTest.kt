package org.spb.project


import org.junit.jupiter.api.Test
import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType
import org.spb.project.presenter.algorithm.DijkstraAlgorithm
import org.junit.jupiter.api.Assertions.*

class DijkstraTest {
    @Test
    fun `simple linear graph`() {
        val g = Graph(GraphType.NON_ORIENTED)

        g.addEdge(0, 1, 1, 0)
        g.addEdge(1, 0, 1, 0)
        g.addEdge(1, 2, 1, 0)
        g.addEdge(2, 1, 1, 0)
        val dijkstra = DijkstraAlgorithm(0,2,g)
        val result = dijkstra.dijkstra(g,dijkstra.arrayEdge())
        assertEquals(listOf(0,1,2), result.second)
        assertEquals(2,result.first)



    }
    @Test
    fun `graph with different weight`() {
        val g = Graph(GraphType.NON_ORIENTED)

        g.addEdge(0, 1, 1, 0)
        g.addEdge(1, 0, 1, 0)
        g.addEdge(1, 2, 1, 0)
        g.addEdge(2, 1, 1, 0)
        g.addEdge(0, 2, 3, 0)
        g.addEdge(2, 0, 3, 0)
        val dijkstra = DijkstraAlgorithm(0,2,g)
        val result = dijkstra.dijkstra(g,dijkstra.arrayEdge())
        assertEquals(listOf(0, 1, 2), result.second)
        assertEquals(2, result.first)

    }
    @Test
    fun `graph with unreachable node`() {
        val g = Graph(GraphType.NON_ORIENTED)
        g.addEdge(0, 1, 1, 0)
        g.addEdge(1, 0, 1, 0)
        g.addEdge(1, 2, 1, 0)
        g.addEdge(2, 1, 1, 0)
        val dijkstra = DijkstraAlgorithm(0, 6, g)
        val result = dijkstra.dijkstra(g, dijkstra.arrayEdge())
        assertEquals(0, result.first)
    }
    @Test
    fun `two unreachable components `() {
        val g = Graph(GraphType.NON_ORIENTED)
        g.addEdge(0, 1, 1, 0)
        g.addEdge(1, 0, 1, 0)
        g.addEdge(1, 2, 1, 0)
        g.addEdge(2, 1, 1, 0)
        g.addEdge(3,4,1,0)
        g.addEdge(4,3,1,0)
        val dijkstra = DijkstraAlgorithm(0, 4, g)
        val result = dijkstra.dijkstra(g, dijkstra.arrayEdge())
        assertEquals(0, result.first)
    }

}
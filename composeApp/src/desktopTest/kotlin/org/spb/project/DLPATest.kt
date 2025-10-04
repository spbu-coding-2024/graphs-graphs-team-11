package org.spb.project

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType
import org.spb.project.presenter.algorithm.DLPA

class DLPATest {

    @Test
    fun `TwoVertexGraphDLPA`() {
        val graph = Graph(GraphType.WEIGHTED_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0, 1, 1, 1)
        val dlpa = DLPA(graph)
        dlpa.labelPropagation()
        assertTrue(dlpa.labels.isNotEmpty())
        assertEquals(2, dlpa.labels.size)
        assertEquals(1, dlpa.labels[0])
        assertEquals(1, dlpa.labels[1])
    }

    @Test
    fun `ThreeVertex1GraphDLPA`() {
        val graph = Graph(GraphType.WEIGHTED_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0, 2, 1, 1)
        graph.addEdge(0, 1, 1, 1)
        val dlpa = DLPA(graph)
        dlpa.labelPropagation()
        assertTrue(dlpa.labels.isNotEmpty())
        assertEquals(3, dlpa.labels.size)
        assertEquals(1, dlpa.labels[0])
        assertEquals(1, dlpa.labels[1])
        assertEquals(2, dlpa.labels[2])
    }

    @Test
    fun `ThreeVertex2GraphDLPA`() {
        val graph = Graph(GraphType.WEIGHTED_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0, 1, 1, 1)
        graph.addEdge(0, 2, 1, 1)
        val dlpa = DLPA(graph)
        dlpa.labelPropagation()
        assertTrue(dlpa.labels.isNotEmpty())
        assertEquals(3, dlpa.labels.size)
        assertEquals(2, dlpa.labels[0])
        assertEquals(1, dlpa.labels[1])
        assertEquals(2, dlpa.labels[2])
    }

    @Test
    fun `FourVertexLineGraphDLPA`() {
        val graph = Graph(GraphType.WEIGHTED_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0, 1, 1, 1)
        graph.addEdge(1, 2, 1, 1)
        graph.addEdge(2, 3, 1, 1)
        val dlpa = DLPA(graph)
        dlpa.labelPropagation()
        assertTrue(dlpa.labels.isNotEmpty())
        assertEquals(4, dlpa.labels.size)
        assertEquals(3, dlpa.labels[0])
        assertEquals(3, dlpa.labels[1])
        assertEquals(3, dlpa.labels[2])
        assertEquals(3, dlpa.labels[3])
    }

    @Test
    fun `K3GraphDLPA`() {
        val graph = Graph(GraphType.WEIGHTED_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0, 1, 1, 1)
        graph.addEdge(0, 2, 1, 1)
        graph.addEdge(1, 0, 1, 1)
        graph.addEdge(1, 2, 1, 1)
        graph.addEdge(2, 0, 1, 1)
        graph.addEdge(2, 1, 1, 1)
        val dlpa = DLPA(graph)
        dlpa.labelPropagation()
        assertTrue(dlpa.labels.isNotEmpty())
        assertEquals(3, dlpa.labels.size)
        assertEquals(2, dlpa.labels[0])
        assertEquals(2, dlpa.labels[1])
        assertEquals(2, dlpa.labels[2])
    }

    @Test
    fun `TriangleAndVertexGraphDLPA`() {
        val graph = Graph(GraphType.WEIGHTED_ORIENTED)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addVertex(1.0, 1.0, 1)
        graph.addEdge(0, 1, 1, 1)
        graph.addEdge(0, 2, 1, 1)
        graph.addEdge(1, 0, 1, 1)
        graph.addEdge(1, 2, 1, 1)
        graph.addEdge(2, 0, 1, 1)
        graph.addEdge(2, 1, 1, 1)
        graph.addEdge(1, 3, 1, 1)
        val dlpa = DLPA(graph)
        dlpa.labelPropagation()
        assertTrue(dlpa.labels.isNotEmpty())
        assertEquals(4, dlpa.labels.size)
        assertEquals(2, dlpa.labels[0])
        assertEquals(2, dlpa.labels[1])
        assertEquals(2, dlpa.labels[2])
        assertEquals(3, dlpa.labels[3])
    }

}

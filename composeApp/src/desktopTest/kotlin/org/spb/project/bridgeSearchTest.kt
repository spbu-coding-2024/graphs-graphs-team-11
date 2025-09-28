package org.spb.project

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType
import org.spb.project.presenter.algorithm.BridgeSearch
import org.spb.project.view.GraphScreen
import javax.imageio.plugins.tiff.BaselineTIFFTagSet

class bridgeSearchTest {
    @Test
    fun `all bridges`(){
        val g = Graph(GraphType.NON_ORIENTED)
        g.addEdge(0,1,0,0)
        g.addEdge(1,0,0,0)
        g.addEdge(1,2,0,0)
        g.addEdge(2,1,0,0)
        val list = BridgeSearch(g)
        val result  = list.bridge()
        assertEquals(result[0],Pair(1,2))
        assertEquals(result[1], Pair(0,1))
    }
    @Test
    fun `no bridges`(){
        val g = Graph(GraphType.NON_ORIENTED)
        g.addEdge(0, 1, 0, 0)
        g.addEdge(1, 2, 0, 0)
        g.addEdge(2, 3, 0, 0)
        g.addEdge(3, 0, 0, 0)
        val list = BridgeSearch(g)
        val result = list.bridge()
        assertEquals(result.size, 0)

    }
    @Test
    fun `two cycles - one bridge`(){
        val g = Graph(GraphType.NON_ORIENTED)
        g.addEdge(0, 1, 0, 0)
        g.addEdge(1, 0, 0, 0)
        g.addEdge(1, 2, 0, 0)
        g.addEdge(2, 1, 0, 0)
        g.addEdge(2, 0, 0, 0)
        g.addEdge(0, 2, 0, 0)

        g.addEdge(3, 4, 0, 0)
        g.addEdge(4, 3, 0, 0)
        g.addEdge(4, 5, 0, 0)
        g.addEdge(5, 4, 0, 0)
        g.addEdge(5, 3, 0, 0)
        g.addEdge(3, 5, 0, 0)

        g.addEdge(2, 3, 0, 0)
        g.addEdge(3, 2, 0, 0)
        val list = BridgeSearch(g)
        val result = list.bridge()
        assertEquals(result[0], Pair(2,3))
    }
    @Test
    fun `two components`(){
        val g = Graph(GraphType.NON_ORIENTED)
        g.addEdge(0, 1, 0, 0)
        g.addEdge(1, 0, 0, 0)
        g.addEdge(3, 4, 0, 0)
        g.addEdge(4, 3, 0, 0)
        val list = BridgeSearch(g)
        val result = list.bridge()
        assertEquals(result.size, 2)
    }
    @Test
    fun `no edges - no bridges`(){
        val g = Graph(GraphType.NON_ORIENTED)
        val list = BridgeSearch(g)
        val result = list.bridge()
        assertEquals(result.size,0)
    }
    }

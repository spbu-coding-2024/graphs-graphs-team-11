package org.spb.project
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spb.project.common.Graph
import org.spb.project.common.GraphType
import org.spb.project.presenter.algorithm.CollectiveInfluence
import org.spb.project.view.GraphScreen

class CITest {
    fun Graph.addBidirectionalEdge(from:Int, to:Int){
        addEdge(from, to, 0,0)
        addEdge(to,from, 0, 0)
    }
    @Test
    fun `lattice 9x9`(){
        val g = Graph(GraphType.NON_ORIENTED)
        g.addBidirectionalEdge(0,1)
        g.addBidirectionalEdge(1,2)
        g.addBidirectionalEdge(3,4)
        g.addBidirectionalEdge(4,5)
        g.addBidirectionalEdge(6,7)
        g.addBidirectionalEdge(7,8)
        g.addBidirectionalEdge(0,3)
        g.addBidirectionalEdge(1,4)
        g.addBidirectionalEdge(2,5)
        g.addBidirectionalEdge(3,6)
        g.addBidirectionalEdge(4,7)
        g.addBidirectionalEdge(5,8)
        val list = CollectiveInfluence(g)
        val result = list.getResultCollectiveInfluence(2)
        assertEquals(2,result.size)
        assertEquals(1,result[0])
        assertEquals(3,result[1])
    }
    @Test
    fun `cycle graph 4 nodes with distance 1`() {
        val g = Graph(GraphType.NON_ORIENTED)
        repeat(4) { g.addVertex(it.toDouble(), it.toDouble()) }

        g.addBidirectionalEdge(1,0)
        g.addBidirectionalEdge(1,2)
        g.addBidirectionalEdge(2,3)
        g.addBidirectionalEdge(3,0)
        val list = CollectiveInfluence(g)
        val result = list.getResultCollectiveInfluence(1)
        assertEquals(1, result.size)
        assertEquals(0, result[0])

    }
    @Test
    fun `linear graph 7 nodes`(){
        val g = Graph(GraphType.NON_ORIENTED)
        g.addBidirectionalEdge(1,0)
        g.addBidirectionalEdge(1,2)
        g.addBidirectionalEdge(2,3)
        g.addBidirectionalEdge(3,4)
        g.addBidirectionalEdge(4,5)
        g.addBidirectionalEdge(5,6)
        val list = CollectiveInfluence(g)
        val result = list.getResultCollectiveInfluence(2)
        assertEquals(0,result.size)
    }
    @Test
    fun `test ci and recalculate ci`(){
        val g = Graph(GraphType.NON_ORIENTED)
        g.addBidirectionalEdge(0,1)
        g.addBidirectionalEdge(1,2)
        g.addBidirectionalEdge(3,4)
        g.addBidirectionalEdge(4,5)
        g.addBidirectionalEdge(6,7)
        g.addBidirectionalEdge(7,8)
        g.addBidirectionalEdge(0,3)
        g.addBidirectionalEdge(1,4)
        g.addBidirectionalEdge(2,5)
        g.addBidirectionalEdge(3,6)
        g.addBidirectionalEdge(4,7)
        g.addBidirectionalEdge(5,8)
        val listTest = CollectiveInfluence(g)
        val ciBefore = listTest.calculateCIforGraph(2)
        assertEquals(12, ciBefore[1])
        assertEquals(12, ciBefore[3])
        assertEquals(5, ciBefore[0])
        val recalculate = listTest.deleteNodeAndRecalculateCI(1,2)
        assertEquals(Pair(0,0), recalculate[0])
        assertEquals(Pair(0,2),recalculate[1])
        assertEquals(Pair(8,3), recalculate[3])
        assertEquals(Pair(4,4), recalculate[2])
        assertEquals(Pair(8,5), recalculate[4])
        assertEquals(Pair(3,6), recalculate[6])
        assertEquals(Pair(8,7), recalculate[5])
        assertEquals(Pair(3,8), recalculate[7])
    }



}
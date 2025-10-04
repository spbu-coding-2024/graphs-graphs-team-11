package org.spb.project.presenter.algorithm

import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType
import kotlin.math.min

open class BridgeSearch(graph: Graph){
    var NonOrientedGraph = graph
    private var edgeList = NonOrientedGraph.getEdges()
    var time = 0
    val resultPair= mutableListOf<Pair<Int,Int>>()
    fun BridgeUtil( nextCurrent:Int, visited:MutableList<Boolean>, disc:MutableList<Int>, low: MutableList<Int>, parent:MutableList<Int>){
        visited[nextCurrent] = true
        time++

        disc[nextCurrent] = time
        low[nextCurrent] = time
        val EdgeListIterator = edgeList[nextCurrent].iterator()
        while(EdgeListIterator.hasNext()){
            val v = EdgeListIterator.next().vertex
            if(!visited[v]){
                parent[v] = nextCurrent
                BridgeUtil(v,visited,disc,low, parent)
                low[nextCurrent] = min(low[nextCurrent], low[v])
                if (low[v]>disc[nextCurrent]){
                    resultPair.add(nextCurrent to v)
                }

            }
            else if (v!=parent[nextCurrent]){
                low[nextCurrent] = min(low[nextCurrent], disc[v])
            }
        }

    }
    fun bridge():List<Pair<Int,Int>>{
        resultPair.clear()
        val visited = MutableList(edgeList.size) { false }
        val disc = MutableList(edgeList.size) { 0 }
        val low = MutableList(edgeList.size) { 0 }
        val parent = MutableList(edgeList.size) { -1 }
        for (i in 0..edgeList.size-1){
            if (!visited[i]){
                (BridgeUtil(i, visited, disc, low, parent))

        }
        }

        return resultPair.toList()
    }


}
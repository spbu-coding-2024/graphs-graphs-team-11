package org.spb.project.presenter.algorithm

import androidx.compose.animation.core.animateValueAsState
import org.spb.project.common.Edge
import org.spb.project.common.Graph
import org.spb.project.common.GraphType
import org.w3c.dom.Node
import kotlin.math.pow

class CollectiveInfluence(graph:Graph) {
    val edges = graph.getEdges()
    fun deleteVertex(vertex:Int){
        if (vertex !in edges.indices) return
        for (i in edges[vertex]){
           edges[i.vertex].removeAll{it.vertex == vertex}
        }
        edges[vertex].clear()
    }
    fun getVertexDegree(vertexId:Int): Int{
        return edges[vertexId].size
    }
    fun getBoundaryNodes(startNode:Int,distance : Int): MutableList<Int>{
        if (distance ==0){
            return mutableListOf(startNode)
        }
        if (distance < 0){
            return mutableListOf()
        }
        var currentLevel: MutableList<Int>  = mutableListOf()
        val visited: MutableList<Int> = mutableListOf()
        visited.add(startNode)
        currentLevel.add(startNode)
        for (i in 1..distance){
            val nextLevel: MutableList<Int> = mutableListOf()
            for (k in currentLevel){
                for (m in edges[k]) {
                    if (m.vertex !in visited) {
                            nextLevel.add(m.vertex)
                            visited.add(m.vertex)
                        }
                }

            }
            currentLevel = nextLevel
            if (i == distance){

                return currentLevel
            }

        }
        return mutableListOf()


    }
    fun calculateCIforNode(startNode: Int,distance: Int):Int{
        val boundaryNodes: MutableList<Int> = getBoundaryNodes(startNode, distance)

        val degreeStartNode = getVertexDegree(startNode) - 1
        var sumDegreeBoundsNodes = 0
        for (i in boundaryNodes){
            sumDegreeBoundsNodes = edges[i].size+sumDegreeBoundsNodes- 1
        }
        val indexCI = degreeStartNode*sumDegreeBoundsNodes

        return indexCI
    }
    fun stop(sumOfValuesCI:Int, distance: Int): Boolean{
        val N = edges.size

        var sumDegree= 0
        for (i in 0..edges.size-1){
            sumDegree = sumDegree+getVertexDegree(i)
        }

        val averageDegree = sumDegree.toDouble()/N
        if (N == 0 || averageDegree == 0.0) throw IllegalArgumentException("Invalid graph")
        val alpha:Double  = (sumOfValuesCI.toDouble()/(N*averageDegree)).pow(1.0/(distance+1))

        println(alpha)
        if (alpha < 1.0){
            return false
        }
        else{
            return true
        }
    }
    fun getNodesForRecalculate(startNode: Int, distance: Int): MutableList<Int>{
        if (distance ==0){
            return mutableListOf(startNode)
        }
        if (distance < 0){
            return mutableListOf()
        }
        val finalList: MutableList<Int>  = mutableListOf()
        var currentLevel: MutableList<Int>  = mutableListOf()
        val visited: MutableList<Int> = mutableListOf()
        visited.add(startNode)
        currentLevel.add(startNode)
        for (i in 1..distance+1){
            val nextLevel: MutableList<Int> = mutableListOf()
            for (k in currentLevel){
                for (m in edges[k]) {
                    if (m.vertex !in visited) {
                        nextLevel.add(m.vertex)
                        visited.add(m.vertex)
                    }
                }

            }
            if (nextLevel.isEmpty()) break
            currentLevel = nextLevel
            finalList.addAll(currentLevel)
        }
        return finalList

    }
    fun deleteNodeAndRecalculateCI(node:Int,distance: Int): MutableList<Pair<Int, Int>>{

        val recalculateNodes = getNodesForRecalculate(node,2)


        val result: MutableList<Pair<Int,Int>> = mutableListOf()

        deleteVertex(node)







        result.addAll(recalculateNodes.map {node1 -> calculateCIforNode(node1,distance) to node1 })

        return result
    }
    fun calculateCIforGraph(distance: Int): MutableList<Int>{
        val result: MutableList<Int> = mutableListOf()
        for (i in edges.indices){
            result.add(calculateCIforNode(i,distance))
        }
        return result
    }
    fun getResultCollectiveInfluence(distance: Int):MutableList<Int>{
        if (edges.size <= 3){
            throw IllegalArgumentException("Graph to small")
        }
        val CI = calculateCIforGraph(distance)
        var sum = CI.sum()
        val resultList: MutableList<Int> = mutableListOf()
        while(stop(sum, 2)){
            resultList.add(CI.indexOf(CI.max()))
            val recalculateCI = deleteNodeAndRecalculateCI(CI.indexOf(CI.max()),distance)

            CI[CI.indexOf(CI.max())] = 0

            for (i in recalculateCI){

                CI[i.second] = i.first
            }

            sum = CI.sum()

        }
        return resultList

    }







}


package org.spb.project.presenter.algorithm
import java.util.PriorityQueue
import org.spb.project.model.common.Graph


class DijkstraAlgorithm(src:Int, target:Int, graph: Graph){
    private val edges = graph.getEdges()
    private val source = src
    private val targetNode = target
    fun arrayEdge(): MutableList<MutableList<Int>> {
        val result = mutableListOf<MutableList<Int>>()
        for ((u, edgesForVertex) in edges.withIndex()) {
            for (edge in edgesForVertex) {
                val edgeList = mutableListOf<Int>()
                edgeList.add(u)
                edgeList.add(edge.vertex)
                edgeList.add(edge.weight)
                result.add(edgeList)
            }
        }

        return result

    }
    fun vertexes(edges: MutableList<MutableList<Int>>, V:Int): MutableList<MutableList<MutableList<Int>>>{

        val adj : MutableList<MutableList<MutableList<Int>>> = mutableListOf()
        for ( i in 0 ..V){
            adj.add(mutableListOf())
        }
        for (edge in edges){
            val u = edge[0]
            val v = edge[1]
            val wt = edge[2]
            val ver = mutableListOf<Int>()
            ver.add(v)
            ver.add(wt)
            adj.get(u).add(ver)
            val ver2 = mutableListOf<Int>()
            ver2.add(u)
            ver2.add(wt)
            adj[v].add(ver2)
        }
        return adj
    }
    fun dijkstra(graph: Graph, edges: MutableList<MutableList<Int>>): Pair<Int, List<Int>> {
        val v = graph.getEdges().size
        val adj = vertexes(edges, v)
        val pq = PriorityQueue<MutableList<Int>>(compareBy { it[0] })
        val dist  = MutableList(v){Int.MAX_VALUE}
        dist[source] = 0
        val start = mutableListOf<Int>()
        val parent = MutableList(v){-1}
        start.add(0)
        start.add(source)
        pq.offer(start)
        while (!pq.isEmpty()){
            val curr: MutableList<Int> = pq.poll()
            val d = curr[0]
            val u = curr[1]
            if (u == targetNode){

                println(dist[targetNode])
                break}
            if (d != dist[u]) continue
            for (neighbor in adj[u]){
                val v = neighbor[0]
                val weight = neighbor[1]

                if (dist[v]>dist[u]+weight){
                    dist[v] = dist[u]+weight
                    parent[v] = u
                    val temp = mutableListOf<Int>()
                    temp.add(dist[v])
                    temp.add(v)
                    pq.offer(temp)
                }
            }
        }
        val path = path(parent, targetNode)
        return dist[targetNode] to path
    }
    fun path (parent: MutableList<Int>, target: Int):List<Int> {
        val path = mutableListOf<Int>()
        var current = target
        while (current!=-1){
            path.add(current)
            current = parent[current]
        }
        return path.reversed()
    }
}

package org.spb.project.presenter.database
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType
import java.lang.AutoCloseable


class neo4jDb(val uri: String?, val user: String, val password: String) : AutoCloseable {
    private val driver: Driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))
    override fun close() {
        driver.close()
    }
    fun saveGraphNeo4j(graph: Graph, graphId:Int = 1) {
        val mapVertexes = graph.getVertexes().mapIndexed { sourceId, vertexList ->
            listOf(sourceId, vertexList.x, vertexList.y, vertexList.color )
        }
        val mapEdges = graph.getEdges().mapIndexed { sourceId, edgesList -> edgesList.map { edge ->
            listOf(
                sourceId,
                edge.vertex,
                edge.weight
            )
        }
        }
            .flatten()
            driver.session().use { session ->
                session.executeWrite { tx ->
                    tx.run(
                        """
                        MATCH (n {graphId:$graphId})-[r]-()
                        DELETE r  
                        """, mapOf("graphId" to graphId)
                    ).consume()
                }
                session.executeWrite { tx ->
                   tx.run(
                        """
                        MATCH (n {graphId:$graphId})
                        DETACH DELETE n    
                        """, mapOf("graphId" to graphId)
                   ).consume()

                }
                session.executeWrite { tx ->
                    tx.run(
                        """
                        UNWIND $mapVertexes AS vertexes     
                        MERGE (nodes:Node{id:vertexes[0], x:vertexes[1], y:vertexes[2], color:vertexes[3], graphId:$graphId})    
                """, mapOf( "mapVertexes" to mapVertexes, "graphId" to graphId)
                    ).consume()
                }
                session.executeWrite { tx->
                    tx.run("""
                        UNWIND $mapEdges AS edges
                        MATCH (source:Node{id:edges[0], graphId:$graphId})
                        MATCH(target:Node{id:edges[1], graphId:$graphId})
                        MERGE (source)-[rel:CONNECTED{weight:edges[2], graphId:$graphId}]->(target)    
                        """, mapOf("mapEdges" to mapEdges, "graphId" to graphId)).consume()
                }
            }

    }
    fun readGraph(graphId:Int = 1 ): Graph {
        val graph = Graph(GraphType.valueOf(GraphType.NON_ORIENTED.name))

            driver.session().use { session ->
                session.executeRead { tx ->
                    val result = tx.run(
                        """
                    MATCH (n {graphId:$graphId})
                    RETURN n
                    """
                    ).list()
                    result.forEach { result ->
                        val node = result.get("n").asNode()
                        graph.addVertex(
                            x = node.get("x").asDouble(),
                            y = node.get("y").asDouble(),
                            color = node.get("color").asInt()
                        )
                    }
                }
                session.executeRead { tx ->
                    val result = tx.run(
                        """
                    MATCH ((a {graphId:$graphId})-[r:CONNECTED{graphId:$graphId}]->(m {graphId:$graphId}))
                    RETURN a,r,m
                    
                """
                    ).list()
                    result.forEach { result ->
                        val node = result.get("a").asNode()
                        val relationship = result.get("r").asRelationship()
                        val relationshipValue = relationship["weight"].asInt()
                        val startVertex = node["id"].asInt()
                        val node2 = result.get("m").asNode()
                        val finalVertex = node2["id"].asInt()

                        graph.addEdge(startVertex,finalVertex,relationshipValue,0xFF888888.toInt())

                    }

                }
            }
        return graph
    }

}
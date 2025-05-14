package org.spb.project.presenter

import common.Graph
import common.GraphType
import java.sql.DriverManager
import java.sql.SQLException

object GraphDbHelper {
    private const val DB_URL = "jdbc:sqlite:graphs.db"

    init {
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS GraphInfo (
                        graph_id INTEGER PRIMARY KEY,
                        type TEXT NOT NULL
                    )
                """.trimIndent()
                )
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS Vertices (
                        graph_id INTEGER,
                        idx INTEGER,
                        x REAL,
                        y REAL,
                        PRIMARY KEY(graph_id, idx)
                    )
                """.trimIndent()
                )
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS Edges (
                        graph_id INTEGER,
                        start_idx INTEGER,
                        end_idx INTEGER,
                        weight INTEGER,
                        color INTEGER
                    )
                """.trimIndent()
                )
                val rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM GraphInfo")
                if (rs.next() && rs.getInt("cnt") == 0) {
                    populateSampleData()
                }
            }


        }

    }



    fun saveGraph(graph: Graph, graphId: Int = 1) {
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("DELETE FROM GraphInfo WHERE graph_id = $graphId")
                    stmt.executeUpdate("DELETE FROM Vertices WHERE graph_id = $graphId")
                    stmt.executeUpdate("DELETE FROM Edges WHERE graph_id = $graphId")
                }
                conn.prepareStatement(
                    "INSERT INTO GraphInfo(graph_id, type) VALUES(?, ?)"
                ).use { ps ->
                    ps.setInt(1, graphId)
                    ps.setString(2, graph.getType().name)
                    ps.executeUpdate()
                }
                conn.prepareStatement(
                    "INSERT INTO Vertices(graph_id, idx, x, y) VALUES(?, ?, ?, ?)"
                ).use { ps ->
                    graph.getVertexes().forEachIndexed { idx, v ->
                        ps.setInt(1, graphId)
                        ps.setInt(2, idx)
                        ps.setDouble(3, v.x)
                        ps.setDouble(4, v.y)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
                conn.prepareStatement(
                    "INSERT INTO Edges(graph_id, start_idx, end_idx, weight, color) VALUES(?, ?, ?, ?, ?)"
                ).use { ps ->
                    graph.getEdges().forEachIndexed { startIdx, list ->
                        list.forEach { e ->
                            ps.setInt(1, graphId)
                            ps.setInt(2, startIdx)
                            ps.setInt(3, e.vertex)
                            ps.setInt(4, e.weight)
                            ps.setInt(5, e.color)
                            ps.addBatch()
                        }
                    }
                    ps.executeBatch()
                }
                conn.commit()
            } catch (ex: SQLException) {
                conn.rollback()
                throw ex
            }
        }
    }

    fun loadGraph(graphId: Int = 1): Graph {
        val graph: Graph
        DriverManager.getConnection(DB_URL).use { conn ->
            // Загрузка типа графа
            val type = conn.prepareStatement(
                "SELECT type FROM GraphInfo WHERE graph_id = ?"
            ).use { ps ->
                ps.setInt(1, graphId)
                ps.executeQuery().let { rs ->
                    if (rs.next()) rs.getString("type") else GraphType.NORMAL.name
                }
            }
            graph = Graph(GraphType.valueOf(type))

            // Загрузка вершин
            conn.prepareStatement(
                "SELECT idx, x, y FROM Vertices WHERE graph_id = ? ORDER BY idx"
            ).use { ps ->
                ps.setInt(1, graphId)
                ps.executeQuery().let { rs ->
                    while (rs.next()) {
                        graph.addVertex(rs.getDouble("x"), rs.getDouble("y"))
                    }
                }
            }

            // Загрузка ребер
            conn.prepareStatement(
                "SELECT start_idx, end_idx, weight, color FROM Edges WHERE graph_id = ?"
            ).use { ps ->
                ps.setInt(1, graphId)
                ps.executeQuery().let { rs ->
                    while (rs.next()) {
                        graph.addEdge(
                            rs.getInt("start_idx"),
                            rs.getInt("end_idx"),
                            rs.getInt("weight"),
                            rs.getInt("color")
                        )
                    }
                }
            }
        }
        return graph
    }

    private fun populateSampleData() {
        // Пример 1: ненаправленный граф
        val g1 = Graph(GraphType.NORMAL)
        g1.addVertex(50.0, 50.0)
        g1.addVertex(150.0, 50.0)
        g1.addVertex(100.0, 150.0)
        g1.addEdge(0, 1, weight = 1, color = 0)
        g1.addEdge(1, 2, weight = 1, color = 0)
        g1.addEdge(2, 0, weight = 1, color = 0)
        saveGraph(g1, graphId = 1)
    }

    /** Возвращает мета-информацию обо всех сохранённых графах */
    fun getAllGraphs(): List<GraphMeta> {
        val result = mutableListOf<GraphMeta>()
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.prepareStatement("SELECT graph_id, type FROM GraphInfo ORDER BY graph_id")
                .use { ps ->
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            result += GraphMeta(
                                id   = rs.getInt("graph_id"),
                                type = GraphType.valueOf(rs.getString("type"))
                            )
                        }
                    }
                }
        }
        return result
    }

    /** Находит максимальный graph_id и возвращает следующий свободный */
    fun getNextGraphId(): Int {
        DriverManager.getConnection(DB_URL).use { conn ->
            conn.prepareStatement("SELECT MAX(graph_id) AS maxId FROM GraphInfo")
                .use { ps ->
                    ps.executeQuery().use { rs ->
                        val max = if (rs.next()) rs.getInt("maxId") else 0
                        return max + 1
                    }
                }
        }
    }
}

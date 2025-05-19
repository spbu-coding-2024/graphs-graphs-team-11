package org.spb.project.presenter

import org.spb.project.common.Graph
import org.spb.project.common.GraphType
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object GraphDbHelper {
    private const val DB_URL = "jdbc:sqlite:graphs.db"
    private const val BUSY_TIMEOUT_MS = 5000
    private const val DEFAULT_VERTEX_COLOR = 0xFF0000FF.toInt()  // синий
    private const val DEFAULT_EDGE_COLOR = 0xFF888888.toInt()  // серый

    init {
        // Инициализация схемы
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS GraphInfo (
                        graph_id INTEGER PRIMARY KEY,
                        type     TEXT    NOT NULL
                    )
                """.trimIndent()
                )
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS Vertices (
                        graph_id INTEGER,
                        idx      INTEGER,
                        x        REAL,
                        y        REAL,
                        color    INTEGER,
                        PRIMARY KEY(graph_id, idx)
                    )
                """.trimIndent()
                )
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS Edges (
                        graph_id  INTEGER,
                        start_idx INTEGER,
                        end_idx   INTEGER,
                        weight    INTEGER,
                        color     INTEGER
                    )
                """.trimIndent()
                )
            }
            // Если пусто — создаём демо-графы 1–4
            getConnection().createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM GraphInfo")
                if (rs.next() && rs.getInt("cnt") == 0) {
                    populateSampleGraph1()
                    populateSampleGraph2()
                    populateSampleGraph3()
                    populateSampleGraph4()
                    populateSampleGraph5()
                    populateSampleGraph6()
                    populateSampleGraph7()
                    populateSampleGraph8()
                }
            }
        }
    }

    /** Открывает соединение и настраивает PRAGMA busy_timeout и WAL */
    private fun getConnection(): Connection {
        val conn = DriverManager.getConnection(DB_URL)
        conn.createStatement().use { stmt ->
            stmt.execute("PRAGMA busy_timeout = $BUSY_TIMEOUT_MS;")
            stmt.execute("PRAGMA journal_mode = WAL;")
        }
        return conn
    }

    /** Сохранить граф (GraphInfo, Vertices, Edges) */
    fun saveGraph(graph: Graph, graphId: Int = 1) {
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { st ->
                    st.executeUpdate("DELETE FROM GraphInfo WHERE graph_id = $graphId")
                    st.executeUpdate("DELETE FROM Vertices  WHERE graph_id = $graphId")
                    st.executeUpdate("DELETE FROM Edges     WHERE graph_id = $graphId")
                }
                conn.prepareStatement(
                    "INSERT INTO GraphInfo(graph_id, type) VALUES(?, ?)"
                ).use { ps ->
                    ps.setInt(1, graphId)
                    ps.setString(2, graph.getType().name)
                    ps.executeUpdate()
                }
                conn.prepareStatement(
                    "INSERT INTO Vertices(graph_id, idx, x, y, color) VALUES(?, ?, ?, ?, ?)"
                ).use { ps ->
                    graph.getVertexes().forEachIndexed { idx, v ->
                        ps.setInt(1, graphId)
                        ps.setInt(2, idx)
                        ps.setDouble(3, v.x)
                        ps.setDouble(4, v.y)
                        ps.setInt(5, v.color)
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

    /** Загрузить граф (тип, вершины, рёбра) */
    fun loadGraph(graphId: Int = 1): Graph {
        val graph: Graph
        getConnection().use { conn ->
            val typeName = conn.prepareStatement(
                "SELECT type FROM GraphInfo WHERE graph_id = ?"
            ).use { ps ->
                ps.setInt(1, graphId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("type") else GraphType.NORMAL.name
                }
            }
            graph = Graph(GraphType.valueOf(typeName))

            conn.prepareStatement(
                "SELECT x, y, color FROM Vertices WHERE graph_id = ? ORDER BY idx"
            ).use { ps ->
                ps.setInt(1, graphId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        graph.addVertex(
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getInt("color")
                        )
                    }
                }
            }

            conn.prepareStatement(
                "SELECT start_idx, end_idx, weight, color FROM Edges WHERE graph_id = ?"
            ).use { ps ->
                ps.setInt(1, graphId)
                ps.executeQuery().use { rs ->
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

    /** Удалить граф и все его вершины/рёбра */
    fun deleteGraph(graphId: Int) {
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { st ->
                    st.executeUpdate("DELETE FROM GraphInfo WHERE graph_id = $graphId")
                    st.executeUpdate("DELETE FROM Vertices  WHERE graph_id = $graphId")
                    st.executeUpdate("DELETE FROM Edges     WHERE graph_id = $graphId")
                }
                conn.commit()
            } catch (ex: SQLException) {
                conn.rollback()
                throw ex
            }
        }
    }

    /** Список всех графов для DropDownMenu */
    fun getAllGraphs(): List<GraphMeta> {
        val list = mutableListOf<GraphMeta>()
        getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT graph_id, type FROM GraphInfo ORDER BY graph_id"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        list += GraphMeta(
                            id = rs.getInt("graph_id"),
                            type = GraphType.valueOf(rs.getString("type"))
                        )
                    }
                }
            }
        }
        return list
    }

    /** Следующий свободный идентификатор = max(graph_id)+1 */
    fun getNextGraphId(): Int {
        getConnection().use { conn ->
            conn.prepareStatement(
                "SELECT MAX(graph_id) AS maxId FROM GraphInfo"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    val max = if (rs.next()) rs.getInt("maxId") else 0
                    return max + 1
                }
            }
        }
    }

    private fun populateSampleGraph1() {
        val g = Graph(GraphType.NORMAL)
        g.addVertex(100.0, 100.0, DEFAULT_VERTEX_COLOR)
        g.addVertex(400.0, 100.0, DEFAULT_VERTEX_COLOR)
        g.addVertex(250.0, 400.0, DEFAULT_VERTEX_COLOR)
        g.addEdge(0, 1, 1, DEFAULT_EDGE_COLOR)
        g.addEdge(1, 2, 1, DEFAULT_EDGE_COLOR)
        g.addEdge(2, 0, 1, DEFAULT_EDGE_COLOR)
        saveGraph(g, 1)
    }

    private fun populateSampleGraph2() {
        val g = Graph(GraphType.NORMAL)
        val coords = listOf(
            100.0 to 100.0,
            500.0 to 100.0,
            500.0 to 500.0,
            100.0 to 500.0
        )
        coords.forEachIndexed { i, (x, y) ->
            g.addVertex(x, y, DEFAULT_VERTEX_COLOR)
            g.addEdge(i, (i + 1) % coords.size, 1, DEFAULT_EDGE_COLOR)
        }
        saveGraph(g, 2)
    }

    private fun populateSampleGraph3() {
        val g = Graph(GraphType.WEIGHTED)
        val cx = 300.0
        val cy = 300.0
        val r = 200.0
        val n = 8
        repeat(n) { i ->
            val angle = 2 * Math.PI * i / n
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            g.addVertex(x, y, DEFAULT_VERTEX_COLOR)
        }
        for (i in 0 until n) {
            g.addEdge(i, (i + 1) % n, 1, DEFAULT_EDGE_COLOR)
        }
        saveGraph(g, 3)
    }

    private fun populateSampleGraph4() {
        val g = Graph(GraphType.NORMAL)
        val count = 200
        val minX = 50.0
        val minY = 50.0
        val maxX = 800.0
        val maxY = 600.0
        val rnd = Random(0)

        repeat(count) {
            val x = minX + rnd.nextDouble() * (maxX - minX)
            val y = minY + rnd.nextDouble() * (maxY - minY)
            g.addVertex(x, y, DEFAULT_VERTEX_COLOR)
        }

        val verts = g.getVertexes()
        for (i in 0 until count) {
            val xi = verts[i].x
            val yi = verts[i].y
            val neighbors = verts.mapIndexed { j, v ->
                j to ((xi - v.x).let { dx -> dx * dx } + (yi - v.y).let { dy -> dy * dy })
            }
                .filter { it.first != i }
                .sortedBy { it.second }
                .take(3)
            neighbors.forEach { (j, _) ->
                g.addEdge(i, j, 1, DEFAULT_EDGE_COLOR)
            }
        }
        saveGraph(g, 4)
    }

    /**
     * Пример 5.
     * Ориентированный граф из 200 вершин,
     * случайно разбросанных в прямоугольнике 0..1000 × 0..800.
     * Каждая вершина имеет 3 случайных исходящих ребра.
     */
    private fun populateSampleGraph5() {
        val g = Graph(GraphType.ORIENTED)
        val rnd = Random(123)
        val nodeCount = 200
        // Добавляем вершины с рандомными координатами
        repeat(nodeCount) {
            val x = rnd.nextDouble() * 1000.0
            val y = rnd.nextDouble() * 800.0
            g.addVertex(x, y, DEFAULT_VERTEX_COLOR)
        }
        // Для каждой вершины создаём 3 выходящих ребра к случайным другим
        for (i in 0 until nodeCount) {
            repeat(3) {
                val j = rnd.nextInt(nodeCount)
                if (j != i) {
                    g.addEdge(i, j, weight = 1, color = DEFAULT_EDGE_COLOR)
                }
            }
        }
        saveGraph(g, 5)
    }

    /**
     * Пример 6.
     * Ориентированный граф из 300 вершин,
     * разбросанных в круге радиуса 500, центр (500, 400).
     * Каждый узел соединён с 4 ближайшими соседями.
     */
    private fun populateSampleGraph6() {
        val g = Graph(GraphType.ORIENTED)
        val rnd = Random(456)
        val nodeCount = 300
        val cx = 500.0
        val cy = 400.0
        val radius = 500.0

        // Добавляем вершины в круге
        repeat(nodeCount) {
            val angle = rnd.nextDouble() * 2 * Math.PI
            val r = sqrt(rnd.nextDouble()) * radius
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            g.addVertex(x, y, DEFAULT_VERTEX_COLOR)
        }
        // Для каждой вершины находим 4 ближайших и делаем ориентированные ребра
        val verts = g.getVertexes()
        for (i in 0 until nodeCount) {
            val distances = verts.mapIndexed { j, v ->
                j to ((verts[i].x - v.x).let { dx -> dx*dx } + (verts[i].y - v.y).let { dy -> dy*dy })
            }
            distances.filter { it.first != i }
                .sortedBy { it.second }
                .take(4)
                .forEach { (j, _) ->
                    g.addEdge(i, j, weight = 1, color = DEFAULT_EDGE_COLOR)
                }
        }
        saveGraph(g, 6)
    }

    /**
     * Пример 7.
     * Взвешенный граф из 500 случайно разбросанных вершин.
     * Каждая вершина соединяется с 5 ближайшими соседями,
     * вес ребра — случайное целое от 1 до 20.
     */
    private fun populateSampleGraph7() {
        val g = Graph(GraphType.WEIGHTED)
        val rnd = Random(7)
        val nodeCount = 500

        // 1) создаём вершины в прямоугольнике 0..1000 × 0..800
        repeat(nodeCount) {
            val x = rnd.nextDouble() * 1000.0
            val y = rnd.nextDouble() * 800.0
            g.addVertex(x, y, DEFAULT_VERTEX_COLOR)
        }

        // 2) для каждой вершины соединяем с 5 ближайшими
        val verts = g.getVertexes()
        for (i in verts.indices) {
            val xi = verts[i].x
            val yi = verts[i].y
            // вычисляем квадраты дистанций до всех остальных
            val nearest = verts
                .mapIndexed { j, v ->
                    j to ((xi - v.x).pow(2) + (yi - v.y).pow(2))
                }
                .filter { it.first != i }
                .sortedBy { it.second }
                .take(5)

            // создаём ребра с весом 1..20
            nearest.forEach { (j, _) ->
                val weight = rnd.nextInt(1, 21)
                g.addEdge(i, j, weight, DEFAULT_EDGE_COLOR)
            }
        }

        saveGraph(g, 7)
    }

    /**
     * Пример 8.
     * Взвешенный «сеточный» граф 20×20 (400 узлов),
     * вершины лежат на равномерной сетке, рёбра между соседями
     * (горизонтально/вертикально) с весами, зависящими от позиции.
     */
    private fun populateSampleGraph8() {
        val g = Graph(GraphType.WEIGHTED)
        val rows = 20
        val cols = 20
        val stepX = 40.0
        val stepY = 40.0

        // создаём сетку вершин
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = 50.0 + c * stepX
                val y = 50.0 + r * stepY
                g.addVertex(x, y, DEFAULT_VERTEX_COLOR)
            }
        }
        // добавляем рёбра к правому и нижнему соседям,
        // вес = (r+c) mod 10 + 1, чтобы были «волнистые» значения
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val baseWeight = (r + c) % 10 + 1
                // вправо
                if (c < cols - 1) {
                    g.addEdge(idx, idx + 1, baseWeight, DEFAULT_EDGE_COLOR)
                }
                // вниз
                if (r < rows - 1) {
                    g.addEdge(idx, idx + cols, baseWeight, DEFAULT_EDGE_COLOR)
                }
            }
        }
        saveGraph(g, 8)
    }
}
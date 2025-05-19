import org.spb.project.common.Graph
import kotlin.math.sqrt

/**
 * Реализация алгоритма ForceAtlas2 с фиксированными параметрами.
 */
class ForceAtlas2Layout(
    private val repulsionConstant: Double = 100.0,
    private val attractionConstant: Double = 0.1,  // увеличенная сила притяжения
    private val damping: Double = 0.9             // повышенный коэффициент демпфирования
) {
    /**
     * Применить один шаг ForceAtlas2 к графу. Обновляет координаты вершин в модели.
     */
    fun applyLayout(graph: Graph) {
        val vertices = graph.getVertexes()
        val edges = graph.getEdges()
        val n = vertices.size
        val forceX = DoubleArray(n) { 0.0 }
        val forceY = DoubleArray(n) { 0.0 }

        // 1) отталкивающие силы между всеми парами (Coulomb)
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val vi = vertices[i]
                val vj = vertices[j]
                val dx = vi.x - vj.x
                val dy = vi.y - vj.y
                val dist2 = dx * dx + dy * dy + 0.01
                val dist = sqrt(dist2)
                val f = repulsionConstant / dist2
                val fx = (dx / dist) * f
                val fy = (dy / dist) * f

                forceX[i] += fx; forceY[i] += fy
                forceX[j] -= fx; forceY[j] -= fy
            }
        }

        // 2) притягивающие силы по рёбрам (Hooke)
        edges.forEachIndexed { i, list ->
            val vi = vertices[i]
            list.forEach { e ->
                val j = e.vertex
                val vj = vertices[j]
                val dx = vj.x - vi.x
                val dy = vj.y - vi.y
                val dist = sqrt(dx * dx + dy * dy) + 0.01
                val f = attractionConstant * dist
                val fx = (dx / dist) * f
                val fy = (dy / dist) * f

                forceX[i] += fx; forceY[i] += fy
                forceX[j] -= fx; forceY[j] -= fy
            }
        }

        // 3) обновление позиций
        for (k in 0 until n) {
            vertices[k].x += forceX[k] * damping
            vertices[k].y += forceY[k] * damping
        }
    }
}
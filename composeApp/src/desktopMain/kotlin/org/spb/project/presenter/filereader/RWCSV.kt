package org.spb.project.presenter.filereader

import java.io.File
import java.awt.FileDialog
import java.awt.Frame
import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType

/**
 * Реализация чтения и записа графа в scv файл
 *
 * Структура файла:
 * Тип графа
 * Перечень вершин
 * Перечень ребер
 */
class RWCSV() {
    var graph = Graph(GraphType.ORIENTED)

    /**
     * Функция вызова окна, в котром выбираем файл из которого читаем или в который записываем данные
     */
    private fun showFileChooser(): String? {
        val dialog = FileDialog(null as Frame?, "Выберите файл", FileDialog.LOAD)
        dialog.isVisible = true

        return if (dialog.file != null) {
            "${dialog.directory}${dialog.file}"
        } else {
            null
        }
    }

    fun write(graph: Graph) {
        val filename = showFileChooser()
        if (filename == null) {
            return
        }

        var edges = graph.getEdges()
        var vertexes = graph.getVertexes()
        File(filename).writeText("${graph.getType()}\n")

        File(filename).appendText("Vertexes\n")
        for (v in vertexes) {
            File(filename).appendText("${v.x};${v.y};${v.color}\n")
        }

        File(filename).appendText("Edges\n")
        for (elem in edges) {
            if (elem.isNotEmpty()) {
                for (j in 0..elem.size - 1) {
                    if (j != elem.size - 1) {
                        File(filename).appendText("${elem[j].vertex};${elem[j].weight};${elem[j].color};")
                    } else {
                        File(filename).appendText("${elem[j].vertex};${elem[j].weight};${elem[j].color}\n")
                    }
                }
            } else {
                File(filename).appendText("\n")
            }
        }

    }

    fun read(): Graph {

        var graph = Graph(GraphType.ORIENTED)
        val filename = showFileChooser()
        if (filename == null) {
            return graph
        }

        val ff = File(filename).readLines()
        var edgeStartIndex = 0

        for (i in 0..ff.size - 1) {
            if (ff[i] == "Edges") {
                edgeStartIndex = i + 1
                break
            }

            if (i == 0){
                if (ff[i] == "NON_ORIENTED") {
                    graph = Graph(GraphType.NON_ORIENTED)
                } else if (ff[i] == "ORIENTED") {
                    graph = Graph(GraphType.ORIENTED)
                } else if (ff[i] == "WEIGHTED_ORIENTED") {
                    graph = Graph(GraphType.WEIGHTED_ORIENTED)
                } else if (ff[i] == "WEIGHTED_NON_ORIENTED") {
                    graph = Graph(GraphType.WEIGHTED_NON_ORIENTED)
                } else return graph
                continue
            }

            if ((ff[i] != "Vertexes")) {
                val strs = ff[i].split(";").toTypedArray()
                if (strs.size != 3) {
                    return graph
                } else {
                    graph.addVertex(strs[0].toDouble(), strs[1].toDouble(), strs[2].toInt())
                }
            }
        }


        for (i in edgeStartIndex..ff.size - 1) {
            if (ff[i] == "") {
                continue
            }
            val strs = ff[i].split(";").toTypedArray()
            if (strs.size % 3 != 0) {
                return graph
            }
            var cnt = 0
            while (cnt < strs.size) {
                graph.addEdge(i - edgeStartIndex, strs[cnt].toInt(), strs[cnt + 1].toInt(), strs[cnt + 2].toInt())
                cnt += 3
            }
        }
        return graph
    }
}


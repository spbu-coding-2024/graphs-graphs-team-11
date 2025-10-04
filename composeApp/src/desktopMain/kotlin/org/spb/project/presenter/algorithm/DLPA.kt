package org.spb.project.presenter.algorithm

import org.spb.project.model.common.Graph
import org.spb.project.model.common.GraphType

/**
 * Реализация алгоритма DLPA (Распространения меток) для поиска сообществ в графе
 * Алгоритм работает и для ориентированных графов
 * Цвет меток выбирается в зависимости от веса ребра (если граф взвешенный)
 *
 * Параметры:
 * @param graph граф, в котором выполняем поиск сообществ
 */
class DLPA(graph: Graph) {

    private val edges = graph.getEdges()
    private val n = graph.getVertexes().size
    private val type = graph.getType()

    /**
     * Список с метками
     */
    var labels = mutableListOf<Int>()

    /**
     * Реализация алгоритма
     *
     * Параметры
     * @param max_iter количество итераций алгоритма, по умолчанию 100
     */
    fun labelPropagation(max_iter: Int = 100) {

        for (i in 0..n - 1) {
            labels.add(i)
        }

        /*
            В данной реализации алгоритма, метка сообщества выбирается, в зависимости от ИСХОДЯЩИХ ребёр
        */
        for (i in 0..max_iter) {

            var changed = false
            for (j in 0..n - 1) {

                if (edges[j].isEmpty()) {
                    continue
                }

                /*
                    Смотрим на метки соседей и подсчитываем количество каждой из меток
                 */

                var neighborLabel = mutableListOf<Int>()
                var numberLabels = mutableListOf<Int>()

                for (neighbor in edges[j]) {
                    if (labels[neighbor.vertex] !in neighborLabel) {
                        neighborLabel.add(labels[neighbor.vertex])
                        if (type == GraphType.WEIGHTED_ORIENTED || type == GraphType.WEIGHTED_NON_ORIENTED) {
                            numberLabels.add(neighbor.weight)
                        } else numberLabels.add(0)
                    } else {
                        for (label in 0..neighborLabel.size - 1) {
                            if (labels[neighbor.vertex] == neighborLabel[label]) {
                                if (type == GraphType.WEIGHTED_ORIENTED || type == GraphType.WEIGHTED_NON_ORIENTED) {
                                    numberLabels[label] += neighbor.weight
                                } else numberLabels[label] += 1
                            }
                        }
                    }
                }

                if (neighborLabel.isEmpty()) {
                    continue
                }

                /*
                    В оригинальной версии алгоритма, метка выбирается случайным образом
                    из меток встречающихся с наибольшей частотой
                    в данной реализации, выбирается метка с самым последним номером с списке меток
                 */

                for (k in neighborLabel.size - 1 downTo 0) {
                    if (numberLabels[k] == numberLabels.max()) {
                        if (labels[j] != neighborLabel[k]) {
                            labels[j] = neighborLabel[k]
                            changed = true
                        }
                        break
                    }
                }
            }
            if (!changed) {
                break
            }
        }
    }

}
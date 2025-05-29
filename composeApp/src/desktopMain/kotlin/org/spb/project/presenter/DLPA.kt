package org.spb.project.presenter

import org.spb.project.common.*

/**
 * Реализация алгоритма DLPA (Распространения меток) для поиска сообществ в графе
 *
 * Параметры:
 * @param graph граф, в котором выполняем поиск сообществ
 */
class DLPA (graph: Graph){

    private val edges = graph.getEdges()
    private val n = graph.getVertexes().size

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
    fun labelPropagation(max_iter: Int = 100){

        for (i in 0..n-1){
            labels.add(i)
        }

        for (i in 0..max_iter){

            var changed = false
            for (j in 0..n-1){

                if (edges[j].isEmpty()){
                    continue
                }
                /*
                    Смотрим на метки соседей и подсчитываем количество каждой из меток
                 */
                var neighborLabel = mutableListOf<Int>()
                var numberLabels = mutableListOf<Int>()
                for (neighbor in edges[j]){
                    if (labels[neighbor.vertex] !in neighborLabel) {
                        neighborLabel.add(labels[neighbor.vertex])
                        numberLabels.add(0)
                    } else {
                        for (label in 0..neighborLabel.size-1) {
                            if (labels[neighbor.vertex] == neighborLabel[label]) {
                                numberLabels[label] += 1
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
                    В данной реализации, выбирается метка с самым последним номером с списке меток
                 */
                for (k in neighborLabel.size-1 downTo 0){
                    if (numberLabels[k] == numberLabels.max()){
                        if (labels[j] != neighborLabel[k]){
                            labels[j] = neighborLabel[k]
                            changed = true
                        }
                    }
                }
            }
            if (!changed){
                break
            }
        }
    }

}
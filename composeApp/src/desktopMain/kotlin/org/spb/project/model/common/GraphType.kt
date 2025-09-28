package org.spb.project.model.common

/**
 * Типы графов, определяющие способ интерпретации рёбер:
 * - NON_ORIENTED: неориентированный граф без весов
 * - ORIENTED: ориентированный граф без весов
 * - WEIGHTED_ORIENTED: ориентированный граф с весами на рёбрах
 * - WEIGHTED_NON_ORIENTED: неориентированный граф с весами на рёбрах
 */
enum class GraphType {
    /** Рёбра не имеют направления и не взвешены */
    NON_ORIENTED,

    /** Рёбра направленные, но без веса */
    ORIENTED,

    /** Рёбра направленные и имеют вес */
    WEIGHTED_ORIENTED,

    /** Рёбра без направления, но с весом */
    WEIGHTED_NON_ORIENTED
}
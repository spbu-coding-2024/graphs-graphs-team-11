package org.spb.project.presenter

import org.spb.project.common.GraphType

/**
 * Короткая модель для списка доступных графов в интерфейсе.
 *
 * @property id   Уникальный идентификатор графа в базе данных.
 * @property type Тип графа — определяет, является ли он ориентированным,
 *                взвешенным и т. п., чтобы показать соответствующий значок или фильтровать список.
 */
data class GraphMeta(
    val id: Int,        // номер записи в БД, по которому можно загрузить конкретный граф
    val type: GraphType // тип графа (NON_ORIENTED, ORIENTED, WEIGHTED_ORIENTED, WEIGHTED_NON_ORIENTED)
)

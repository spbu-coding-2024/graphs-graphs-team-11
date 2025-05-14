package org.spb.project.presenter

import org.spb.project.common.GraphType

// Простая модель для отображения списка графов в UI
// Хранит идентификатор графа и его тип
data class GraphMeta(
    val id: Int,            // уникальный номер графа в базе данных
    val type: GraphType     // тип графа: NORMAL, ORIENTED или WEIGHTED
)
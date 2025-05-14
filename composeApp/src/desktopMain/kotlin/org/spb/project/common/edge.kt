package org.spb.project.common

/*
    Класс ребра
    Первый аргумент - конечная вершина
    Второй аргумент - вес ребра
    Третий аргумент - цвет ребра
*/
open class Edge(
    var vertex: Int,
    var weight: Int,
    var color: Int,
)
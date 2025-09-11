package org.spb.project.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.spb.project.presenter.CanvasPresenter
import org.spb.project.presenter.GraphDbHelper

class CanvasPresenterIntegrationTest {

    private lateinit var presenter: CanvasPresenter

    @BeforeEach
    fun setUp() {
        presenter = CanvasPresenter()
        // каждый тест на чистом графе
        val newId = GraphDbHelper.getNextGraphId()
        presenter.loadGraph(newId)
    }

    @Test
    fun addCircleIncreasesCircleNodesListByOne() {
        val beforeCount = presenter.circleNodes.size
        presenter.addCircle(color = 0x11223344)
        assertEquals(
            beforeCount + 1,
            presenter.circleNodes.size,
            "addCircle должен увеличивать количество узлов на 1"
        )
    }

    @Test
    fun selectNodeAndPaintSelectedNodeChangesOnlyThatNodeColor() {
        presenter.addCircle(0xAAAABBBB.toInt())
        presenter.addCircle(0xCCCCDDDD.toInt())
        val targetIndex = presenter.circleNodes.lastIndex

        presenter.selectNode(targetIndex)
        val newColor = 0x12345678
        presenter.paintSelectedNode(newColor)

        presenter.circleNodes.forEachIndexed { index, node ->
            if (index == targetIndex) {
                assertEquals(
                    newColor,
                    node.color,
                    "paintSelectedNode должен менять цвет выбранного узла"
                )
            } else {
                assertNotEquals(
                    newColor,
                    node.color,
                    "paintSelectedNode не должен менять цвет остальных узлов"
                )
            }
        }
    }

    @Test
    fun paintAllSetsSameColorForEveryNode() {
        presenter.addCircle(0x11111111)
        presenter.addCircle(0x22222222)
        presenter.addCircle(0x33333333)
        val uniformColor = 0xFFEEDDCC.toInt()

        presenter.paintAll(uniformColor)

        assertTrue(
            presenter.circleNodes.all { it.color == uniformColor },
            "paintAll должен установить указанный цвет для всех узлов"
        )
    }

    @Test
    fun deleteSelectedNodeRemovesOnlyTheSelectedNode() {
        presenter.addCircle(0xAAAA0000.toInt())
        presenter.addCircle(0xBBBB0000.toInt())
        presenter.addCircle(0xCCCC0000.toInt())
        val initialColors = presenter.circleNodes.map { it.color }

        presenter.selectNode(1)
        presenter.deleteSelectedNode()

        assertEquals(
            initialColors.size - 1,
            presenter.circleNodes.size,
            "deleteSelectedNode должен уменьшать список узлов на 1"
        )

        val expectedColors = listOf(initialColors[0], initialColors[2])
        val actualColors = presenter.circleNodes.map { it.color }
        assertEquals(
            expectedColors,
            actualColors,
            "После удаления выбранного узла остальные должны сместиться и сохраниться в том же порядке"
        )
    }
    @Test
    fun `adding a vertex also adds an edge`(){
        presenter.addCircle(0xAAAA0000.toInt())
        presenter.addCircle(0xBBBB0000.toInt())
        presenter.selectNode(0)
        presenter.addCircle(1)
        assertEquals(presenter.edges[0].size,1)
        assertEquals(presenter.edges[0][0].vertex, 2)

    }
    @Test
    fun `deleting a vertex results in deleting all edges`(){
        presenter.addCircle(0xAAAA0000.toInt())
        presenter.addCircle(0xBBBB0000.toInt())
        presenter.selectNode(1)
        presenter.addCircle(0xBBBB0000.toInt())
        presenter.addCircle(0xCCCC0000.toInt())
        presenter.addCircle(0xDDDD0000.toInt())
        presenter.addCircle(0xAAAB0000.toInt())
        presenter.deleteSelectedNode()
        for (i in presenter.edges){
            assertEquals(i.size,0, "После удаления вершины список ребер будет пуст" )
        }
    }
}
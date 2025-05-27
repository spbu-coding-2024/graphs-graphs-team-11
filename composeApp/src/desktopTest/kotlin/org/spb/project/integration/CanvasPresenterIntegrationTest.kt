package org.spb.project.integration

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.spb.project.presenter.CanvasPresenter
import org.spb.project.presenter.GraphDbHelper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanvasPresenterIntegrationTests {

    private lateinit var presenter: CanvasPresenter
    private var testGraphId: Int = -1

    @BeforeEach
    fun setUp() {
        // берём чистый идентификатор и грузим пустой граф
        testGraphId = GraphDbHelper.getNextGraphId()
        presenter = CanvasPresenter()
        presenter.loadGraph(testGraphId)
    }

    @AfterEach
    fun tearDown() {
        // удаляем тестовый граф из БД, чтобы не засорять
        GraphDbHelper.deleteGraph(testGraphId)
    }

    @Test
    fun saveAndLoadGraphRestoresState() {
        // 1) подготовка: добавим 2 вершины и покрасим их по-разному
        presenter.addCircle(color = 0xAA000011.toInt())
        presenter.addCircle(color = 0xAA110000.toInt())
        // нарисуем ещё все в единый цвет для проверки
        presenter.paintAll(0xFF123456.toInt())

        // запоминаем текущее состояние
        val expectedColors = presenter.circleNodes.map { it.color }
        val expectedCount = presenter.circleNodes.size

        // 2) сохраняем граф
        presenter.saveGraph(testGraphId)

        // 3) создаём новый презентер и загружаем из БД
        val reloaded = CanvasPresenter()
        reloaded.loadGraph(testGraphId)

        // 4) проверяем, что при загрузке восстановился точно тот же список узлов
        assertEquals(
            expectedCount, reloaded.circleNodes.size,
            "При загрузке из БД должно восстанавливаться ровно $expectedCount узлов"
        )
        assertEquals(
            expectedColors, reloaded.circleNodes.map { it.color },
            "Цвета всех узлов после loadGraph должны совпадать с сохранёнными"
        )
    }
}
package org.spb.project

import org.spb.project.presenter.ui.CanvasPresenter
import org.spb.project.view.GraphScreen
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Точка входа в десктопное приложение на Compose.
 * Создаёт окно, максимально развёрнутое на весь экран,
 * и отображает в нём наш главный экран GraphScreen.
 */
fun main() = application {
    // Инициализируем презентер один раз и храним его при пересоздании UI
    val presenter = remember { CanvasPresenter() }

    Window(
        onCloseRequest = ::exitApplication,                      // выход из приложения при закрытии окна
        title = "Graph Analyzer",                                // заголовок окна
        state = rememberWindowState(placement = WindowPlacement.Maximized) // по умолчанию — во весь экран
    ) {
        // Внутри окна рендерим основной экран и передаём ему презентер
        GraphScreen(presenter)
    }
}
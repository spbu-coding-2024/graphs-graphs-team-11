package org.spb.project

import org.spb.project.presenter.CanvasPresenter
import org.spb.project.view.GraphScreen
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState


fun main() = application {
    val presenter = remember { CanvasPresenter() }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graph Analyzer",
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        GraphScreen(presenter)
    }
}
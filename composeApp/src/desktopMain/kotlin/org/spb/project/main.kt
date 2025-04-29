package org.spb.project

import CanvasPresenter
import DraggableCanvasView
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val presenter = remember { CanvasPresenter() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        DraggableCanvasView(presenter)
    }
}
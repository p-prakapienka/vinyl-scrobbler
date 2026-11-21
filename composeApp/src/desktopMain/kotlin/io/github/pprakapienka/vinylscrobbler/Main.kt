package io.github.pprakapienka.vinylscrobbler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Vinyl Scrobbler",
    ) {
        App()
    }
}

package ru.uust.schedule

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.uust.schedule.ui.ScheduleApp
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 840.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Расписание УУНиТ",
        icon = painterResource("icon.png"),
        state = windowState,
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(900, 620)
        }
        ScheduleApp()
    }
}

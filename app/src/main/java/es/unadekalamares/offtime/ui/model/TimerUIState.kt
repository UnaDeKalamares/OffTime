package es.unadekalamares.offtime.ui.model

data class TimerUIState(
    val topTimer: TimerState = TimerState(),
    val bottomTimer: TimerState = TimerState()
) {
    fun isTopRunning(): Boolean = topTimer.status == TimerStatus.Running
    fun isBottomRunning(): Boolean = bottomTimer.status == TimerStatus.Running
    fun areAllPaused(): Boolean = topTimer.status == TimerStatus.Paused && bottomTimer.status == TimerStatus.Paused
}
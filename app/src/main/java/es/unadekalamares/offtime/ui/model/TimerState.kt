package es.unadekalamares.offtime.ui.model

data class TimerState(
    val status: TimerStatus = TimerStatus.Stopped,
    val timer: String = DEFAULT_TIMER
) {

    companion object {
        const val DEFAULT_TIMER: String = "00:00:00"
    }

}
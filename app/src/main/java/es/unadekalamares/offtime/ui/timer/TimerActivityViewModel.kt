package es.unadekalamares.offtime.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.unadekalamares.offtime.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class TimerActivityViewModel : ViewModel(), KoinComponent {

    private var formattedTopTimer: String = "00:00:00"
    private var formattedBottomTimer: String = "00:00:00"

    private var _timerUIState: MutableStateFlow<TimerUIState> = MutableStateFlow(TimerUIState())
    val timerUIState: StateFlow<TimerUIState> =_timerUIState.asStateFlow()

    fun listenToService(service: TimerService) {
        viewModelScope.launch {
            for (newTimer in service.timerChannel) {
                formattedTopTimer = processTime(newTimer.first) ?: formattedTopTimer
                formattedBottomTimer = processTime(newTimer.second) ?: formattedBottomTimer
                val newState = TimerUIState(
                    formattedTopTimer,
                    formattedBottomTimer
                )
                _timerUIState.value = newState
            }
        }
    }

    private fun processTime(time: Long): String? {
        if (time % 1000 == 0L) {
            val seconds = (time / 1000)
            val minutes = (seconds / 60)
            val hours = (minutes / 60)

            val roundedSeconds = seconds % 60
            val roundedMinutes = minutes % 60
            val roundedHours = hours % 60
            val formattedTime = String.format(
                "%02d:%02d:%02d",
                roundedHours,
                roundedMinutes,
                roundedSeconds
            )
            return formattedTime
        } else {
            return null
        }
    }

}
package es.unadekalamares.offtime.ui.timer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.unadekalamares.offtime.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class TimerActivityViewModel : ViewModel(), KoinComponent {

    private var _timerUIState: MutableStateFlow<TimerUIState> = MutableStateFlow(TimerUIState())
    val timerUIState: StateFlow<TimerUIState> =_timerUIState.asStateFlow()

    fun listenToService(service: TimerService) {
        viewModelScope.launch {
            for (topTimer in service.topTimerChannel) {
                Log.i("TIMER SERVICE", "Received timer: $topTimer")
                if (topTimer % 1000 == 0L) {
                    val seconds = (topTimer / 1000) % 60
                    val minutes = (seconds / 60) % 60
                    val hours = (minutes / 60) % 60
                    val newState = TimerUIState(
                        topTimer = "$hours:$minutes:$seconds"
                    )
                    _timerUIState.value = newState
                }
            }
            service.topTimerStateFlow.collect { topTimer ->
                val newState = TimerUIState(
                    topTimer = topTimer.toString()
                )
                _timerUIState.value = newState
            }
        }
    }

}
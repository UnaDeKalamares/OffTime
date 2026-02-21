package es.unadekalamares.offtime.timer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerActivityViewModel : ViewModel() {

    private var _timerUIState: MutableStateFlow<TimerUIState> = MutableStateFlow(TimerUIState())
    val timerUIState: StateFlow<TimerUIState> =_timerUIState.asStateFlow()

}
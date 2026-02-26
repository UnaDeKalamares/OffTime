package es.unadekalamares.offtime.ui.timer

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.unadekalamares.offtime.R
import es.unadekalamares.offtime.notification.NotificationsHelper
import es.unadekalamares.offtime.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TimerActivityViewModel : ViewModel(), KoinComponent {

    private val notificationHelper: NotificationsHelper by inject()

    private var isTopTimerRunning: Boolean = true

    private var formattedTopTimer: String = "00:00:00"
    private var formattedBottomTimer: String = "00:00:00"

    private var lastProcessedSeconds: Long = 0

    private var _timerUIState: MutableStateFlow<TimerUIState> = MutableStateFlow(TimerUIState())
    val timerUIState: StateFlow<TimerUIState> =_timerUIState.asStateFlow()

    fun setTopTimerRunning(isTopRunning: Boolean) {
        isTopTimerRunning = isTopRunning
    }

    fun listenToService(service: TimerService, context: Context) {
        viewModelScope.launch {
            for (newTimer in service.timerChannel) {
                formattedTopTimer = processTime(newTimer.first) ?: formattedTopTimer
                formattedBottomTimer = processTime(newTimer.second) ?: formattedBottomTimer
                val newState = TimerUIState(
                    formattedTopTimer,
                    formattedBottomTimer
                )
                _timerUIState.value = newState
                updateNotification(context)
            }
        }
    }

    private fun processTime(time: Long): String? {
        val seconds = (time / 1000)
        if (lastProcessedSeconds != seconds) {
            lastProcessedSeconds = seconds
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
            Log.i("TimerService", "Time: $time; Minutes: $minutes; Seconds: $seconds")
            return formattedTime
        } else {
            return null
        }
    }

    private fun updateNotification(context: Context) {
        try {
            val text = if (isTopTimerRunning) {
                context.getString(R.string.notification_top_timer_text, formattedTopTimer)
            } else {
                context.getString(R.string.notification_bottom_timer_text, formattedBottomTimer)
            }
            notificationHelper.updateNotification(
                text,
                context
            )
        } catch (e: SecurityException) {

        }
    }

}
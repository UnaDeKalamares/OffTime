package es.unadekalamares.offtime.ui.timer

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.unadekalamares.offtime.R
import es.unadekalamares.offtime.notification.NotificationsHelper
import es.unadekalamares.offtime.permissions.PermissionStatus
import es.unadekalamares.offtime.permissions.PermissionsManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TimerActivityViewModel : ViewModel(), KoinComponent {

    private val notificationHelper: NotificationsHelper by inject()
    private val permissionsManager: PermissionsManager by inject()

    private var isTopTimerRunning: Boolean = true

    val DEFAULT_TIMER: String = "00:00:00"

    private var formattedTopTimer: String = DEFAULT_TIMER
    private var formattedBottomTimer: String = DEFAULT_TIMER

    private var lastProcessedSeconds: Long = 0

    private var _timerUIState: MutableStateFlow<TimerUIState> = MutableStateFlow(TimerUIState())
    val timerUIState: StateFlow<TimerUIState> =_timerUIState.asStateFlow()

    fun setTopTimerRunning(isTopRunning: Boolean) {
        isTopTimerRunning = isTopRunning
    }

    fun listenToServiceChannel(channel: Channel<Pair<Long, Long>>, activity: TimerActivity) {
        viewModelScope.launch {
            for (newTimer in channel) {
                formattedTopTimer = processTime(newTimer.first) ?: formattedTopTimer
                formattedBottomTimer = processTime(newTimer.second) ?: formattedBottomTimer
                val newState = TimerUIState(
                    formattedTopTimer,
                    formattedBottomTimer
                )
                _timerUIState.value = newState
                tryUpdateNotification(activity)
            }
        }
    }

    fun resetTimers() {
        formattedTopTimer = DEFAULT_TIMER
        formattedBottomTimer = DEFAULT_TIMER
        val newState = TimerUIState(
            formattedTopTimer,
            formattedBottomTimer
        )
        _timerUIState.value = newState
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

    fun tryUpdateNotification(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModelScope.launch {
                val status = permissionsManager.checkPermissionImmediate(activity)
                if (status is PermissionStatus.Granted) {
                    performUpdateNotification(activity)
                }
            }
        } else {
            performUpdateNotification(activity)
        }
    }

    private fun performUpdateNotification(activity: Activity) {
        try {
            val text = if (isTopTimerRunning) {
                activity.getString(R.string.notification_top_timer_text, formattedTopTimer)
            } else {
                activity.getString(
                    R.string.notification_bottom_timer_text,
                    formattedBottomTimer
                )
            }
            notificationHelper.updateNotification(
                text,
                activity
            )
        } catch (e: SecurityException) {
            // Won't happen, already verified
        }
    }

}
package es.unadekalamares.offtime.ui.timer

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.unadekalamares.offtime.R
import es.unadekalamares.offtime.data.service.TimerService
import es.unadekalamares.offtime.domain.notification.NotificationsHelper
import es.unadekalamares.offtime.domain.permissions.PermissionStatus
import es.unadekalamares.offtime.domain.permissions.PermissionsManager
import es.unadekalamares.offtime.ui.model.RunningTimer
import es.unadekalamares.offtime.ui.model.TimerDataParser
import es.unadekalamares.offtime.ui.model.TimerState
import es.unadekalamares.offtime.ui.model.TimerStatus
import es.unadekalamares.offtime.ui.model.TimerUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TimerActivityViewModel : ViewModel(), KoinComponent {

    private val notificationHelper: NotificationsHelper by inject()
    private val permissionsManager: PermissionsManager by inject()
    private val timeDataParser: TimerDataParser by inject()

    private lateinit var timerServiceBinder: TimerService.LocalBinder

    private var topTimerStatus: TimerStatus = TimerStatus.Stopped
    private var bottomTimerStatus: TimerStatus = TimerStatus.Stopped
    private var lastTimerUIState : TimerUIState = TimerUIState()

    private var lastTopSeconds: Long = 0
    private var lastBottomSeconds: Long = 0

    private var _timerUIState: MutableStateFlow<TimerUIState> = MutableStateFlow(lastTimerUIState)
    val timerUIState: StateFlow<TimerUIState> =_timerUIState.asStateFlow()

    fun initServiceBinder(binder: TimerService.LocalBinder, activity: Activity) {
        timerServiceBinder = binder
        listenToServiceChannel(activity)
    }

    private fun listenToServiceChannel(activity: Activity) {
        viewModelScope.launch {
            for (newTimer in timerServiceBinder.getTimerChannel()) {
                val formattedTopTimer = processTime(newTimer.first, RunningTimer.TopTimer)
                val formattedBottomTimer = processTime(newTimer.second, RunningTimer.BottomTimer)
                processNewUIState(
                    formattedTopTimer ?: lastTimerUIState.topTimer.timer,
                    formattedBottomTimer ?: lastTimerUIState.bottomTimer.timer
                )
                if (formattedTopTimer != null || formattedBottomTimer != null) {
                    tryUpdateNotification(activity)
                }
            }
        }
    }

    private fun processTime(time: Long, runningTimer: RunningTimer): String? {
        val seconds = timeDataParser.getSeconds(time)
        when (runningTimer) {
            RunningTimer.TopTimer -> {
                if (seconds != lastTopSeconds) {
                    lastTopSeconds = seconds
                    return timeDataParser.toReadableTime(time)
                } else {
                    return null
                }
            }
            else -> {
                if (seconds != lastBottomSeconds) {
                    lastBottomSeconds = seconds
                    return timeDataParser.toReadableTime(time)
                } else {
                    return null
                }
            }
        }
    }

    fun processNewUIState(formattedTopTimer: String, formattedBottomTimer: String) {
        val newUiState = TimerUIState(
            topTimer = TimerState(
                status = topTimerStatus,
                timer = formattedTopTimer
            ),
            bottomTimer = TimerState(
                status = bottomTimerStatus,
                timer = formattedBottomTimer
            )
        )
        emitUIState(newUiState)
    }

    fun emitUIState(uiState: TimerUIState) {
        lastTimerUIState = uiState
        _timerUIState.value = uiState
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
            val text = if (lastTimerUIState.isTopRunning()) {
                Log.i("ViewModel", "Update top timer")
                activity.getString(
                    R.string.notification_top_timer_text,
                    lastTimerUIState.topTimer.timer
                )
            } else if (lastTimerUIState.isBottomRunning()) {
                Log.i("ViewModel", "Update bottom timer")
                activity.getString(
                    R.string.notification_bottom_timer_text,
                    lastTimerUIState.bottomTimer.timer
                )
            } else {
                Log.i("ViewModel", "Update pause")
                activity.getString(R.string.notification_paused_timer_text)
            }
            notificationHelper.updateNotification(
                text,
                activity
            )
        } catch (e: SecurityException) {
            // Won't happen, already verified
        }
    }

    fun notifyTimerStarted(runningTimer: RunningTimer) {
        when (runningTimer) {
            RunningTimer.TopTimer -> {
                topTimerStatus = TimerStatus.Running
                bottomTimerStatus = TimerStatus.Paused
            }
            RunningTimer.BottomTimer -> {
                topTimerStatus = TimerStatus.Paused
                bottomTimerStatus = TimerStatus.Running
            }
            RunningTimer.None -> {
                // Do nothing
            }
        }
    }

    fun pauseTimers(activity: Activity) {
        timerServiceBinder.pause()
        topTimerStatus = TimerStatus.Paused
        bottomTimerStatus = TimerStatus.Paused
        processNewUIState(
            lastTimerUIState.topTimer.timer,
            lastTimerUIState.bottomTimer.timer
        )
        tryUpdateNotification(activity)
    }

    fun stopTimers() {
        if (this::timerServiceBinder.isInitialized) {
            timerServiceBinder.stopService()
        }
        emitUIState(TimerUIState())
    }

}
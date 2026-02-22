package es.unadekalamares.offtime.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import es.unadekalamares.offtime.notification.NotificationsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TimerService : LifecycleService() {

    companion object {
        const val IS_TOP_ARG = "IS_TOP_ARG"
    }

    private val notificationsHelper: NotificationsHelper by inject()

    private var topTimerMillis: Long = 0
    private var bottomTimerMillis: Long = 0

    private val _topTimerStateFlow: MutableStateFlow<Long> = MutableStateFlow(0)
    val topTimerStateFlow: StateFlow<Long> = _topTimerStateFlow.asStateFlow()

    private val _bottomTimerStateFlow: MutableStateFlow<Long> = MutableStateFlow(0)
    val bottomTimerStateFlow: StateFlow<Long> = _bottomTimerStateFlow.asStateFlow()

    private var currentTimer: Long = 0
    private var currentStateFlow: MutableStateFlow<Long>? = null

    private var timerJob: Job? = null


    override fun onCreate() {
        super.onCreate()
        timerJob = lifecycleScope.launch {
            delay(100)
            currentTimer += 100
            currentStateFlow?.value = currentTimer
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val isTop = it.getBooleanExtra(IS_TOP_ARG, false)
            if (isTop) {
                currentTimer = topTimerMillis
                currentStateFlow = _topTimerStateFlow
            } else {
                currentTimer = bottomTimerMillis
                currentStateFlow = _bottomTimerStateFlow
            }

            notificationsHelper.createNotificationChannel(this@TimerService)
            val notification = notificationsHelper.buildNotification(this@TimerService)
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }

}
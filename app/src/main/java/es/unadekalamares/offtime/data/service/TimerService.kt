package es.unadekalamares.offtime.data.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import es.unadekalamares.offtime.R
import es.unadekalamares.offtime.domain.notification.NotificationsHelper
import es.unadekalamares.offtime.domain.notification.NotificationsHelper.NOTIFICATION_ID
import es.unadekalamares.offtime.ui.model.RunningTimer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Timer
import java.util.TimerTask

class TimerService : LifecycleService() {

    companion object {
        const val RUNNING_TIMER = "RUNNING_TIMER"
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getTimerChannel(): Channel<Pair<Long, Long>> = timerChannel

        fun stopService() {
            currentRunningTimer = RunningTimer.None
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        fun pause() {
            currentRunningTimer = RunningTimer.None
        }
    }

    private val notificationsHelper: NotificationsHelper by inject()

    private var isServiceStarted: Boolean = false
    private var currentRunningTimer: RunningTimer = RunningTimer.None

    private var topTimerMillis: Long = 0
    private var topTimerLastSync: Long = 0
    private var bottomTimerMillis: Long = 0
    private var bottomTimerLastSync: Long = 0

    private val timerChannel: Channel<Pair<Long, Long>> = Channel(CONFLATED)

    private var timer: Timer? = null

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    lifecycleScope.launch {
                        when (currentRunningTimer) {
                            RunningTimer.None -> {
                                // Do nothing
                            }
                            else -> processNewTime(currentRunningTimer)
                        }

                    }
                }
            }, 0, 100)
        }
    }

    private suspend fun processNewTime(runningTimer: RunningTimer) {
        val currentTime = System.currentTimeMillis()
        if (runningTimer.isTopTimer()) {
            topTimerMillis += currentTime - topTimerLastSync
            topTimerLastSync = currentTime
        } else {
            bottomTimerMillis += currentTime - bottomTimerLastSync
            bottomTimerLastSync = currentTime
        }
        timerChannel.send(Pair(topTimerMillis, bottomTimerMillis))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            currentRunningTimer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra<RunningTimer>(
                    RUNNING_TIMER,
                    RunningTimer::class.java
                ) ?: RunningTimer.None
            } else {
                (intent.getSerializableExtra(
                    RUNNING_TIMER
                ) ?: RunningTimer.None) as RunningTimer
            }

            if (currentRunningTimer.isTopTimer()) {
                topTimerLastSync = System.currentTimeMillis()
            } else if (currentRunningTimer.isBottomTimer()) {
                bottomTimerLastSync = System.currentTimeMillis()
            }

            if (!isServiceStarted) {
                isServiceStarted = true
                notificationsHelper.createNotificationChannel(this@TimerService)
                val notification = notificationsHelper.buildNotification(
                    this@TimerService.getString(R.string.timer_notification_text),
                    this@TimerService
                )
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                )
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun stopService(name: Intent?): Boolean {
        isServiceStarted = false
        return super.stopService(name)
    }

    override fun onDestroy() {
        timer?.cancel()
        timerChannel.cancel()
        super.onDestroy()
    }

}
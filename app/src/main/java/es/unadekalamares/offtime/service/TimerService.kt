package es.unadekalamares.offtime.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import es.unadekalamares.offtime.notification.NotificationsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TimerService : LifecycleService() {

    companion object {
        const val IS_TOP_ARG = "IS_TOP_ARG"
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val notificationsHelper: NotificationsHelper by inject()

    private var isTopTimerRunning: Boolean = true

    private var topTimerMillis: Long = 0
    private var bottomTimerMillis: Long = 0

    val timerChannel: Channel<Pair<Long, Long>> = Channel(CONFLATED)

    private var timerJob: Job? = null

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        timerJob = lifecycleScope.launch {
            while (true) {
                delay(100)
                if (isTopTimerRunning) {
                    topTimerMillis += 100
                } else {
                    bottomTimerMillis += 100
                }
                timerChannel.send(Pair(topTimerMillis, bottomTimerMillis))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val isTop = it.getBooleanExtra(IS_TOP_ARG, false)
            isTopTimerRunning = isTop

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

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        timerChannel.cancel()
        super.onDestroy()
    }

}
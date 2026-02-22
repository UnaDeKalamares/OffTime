package es.unadekalamares.offtime.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import es.unadekalamares.offtime.R
import org.koin.java.KoinJavaComponent.inject

object NotificationsHelper {

    private val notificationManager: NotificationManagerCompat by inject(NotificationManagerCompat::class.java)

    private const val TIMER_NOTIFICATION_CHANNEL_ID = "timer_notification_channel_id"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(
            TIMER_NOTIFICATION_CHANNEL_ID,
            NotificationManager.IMPORTANCE_DEFAULT
        ).setName(context.getString(R.string.timer_notification_channel))
            .build()
        notificationManager.createNotificationChannelsCompat(listOf(channel))
    }

    fun buildNotification(context: Context): Notification =
        NotificationCompat.Builder(context, TIMER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(
                context.getString(R.string.timer_notification_title)
            )
            .setContentText(
                context.getString(R.string.timer_notification_text)
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

}
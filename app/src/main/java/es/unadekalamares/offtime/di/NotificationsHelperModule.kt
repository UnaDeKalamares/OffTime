package es.unadekalamares.offtime.di

import androidx.core.app.NotificationManagerCompat
import org.koin.dsl.module

val notificationsHelperModule = module {
    single { NotificationManagerCompat.from(get()) }
}
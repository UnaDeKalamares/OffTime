package es.unadekalamares.offtime.di

import androidx.core.app.NotificationManagerCompat
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationsHelperModule = module {
    single { NotificationManagerCompat.from(androidContext()) }
}
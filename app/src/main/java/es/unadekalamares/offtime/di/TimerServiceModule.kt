package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.notification.NotificationsHelper
import org.koin.dsl.module

val timerServiceModule = module {
    single { NotificationsHelper }
}
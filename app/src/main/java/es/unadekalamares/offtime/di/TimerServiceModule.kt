package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.domain.notification.NotificationsHelper
import org.koin.dsl.module

val timerServiceModule = module {
    single { NotificationsHelper }
}
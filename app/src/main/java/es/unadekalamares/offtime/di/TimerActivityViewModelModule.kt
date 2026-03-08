package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.domain.notification.NotificationsHelper
import es.unadekalamares.offtime.domain.permissions.PermissionsManager
import es.unadekalamares.offtime.ui.model.TimerDataParser
import org.koin.dsl.module

val timerActivityViewModelModule = module {
    single { NotificationsHelper }
    single { PermissionsManager }
    single { TimerDataParser }
}
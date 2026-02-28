package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.notification.NotificationsHelper
import es.unadekalamares.offtime.permissions.PermissionsManager
import org.koin.dsl.module

val timerActivityViewModelModule = module {
    single { NotificationsHelper }
    single { PermissionsManager }
}
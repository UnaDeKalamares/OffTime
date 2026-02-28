package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.permissions.PermissionsManager
import es.unadekalamares.offtime.ui.timer.TimerActivityViewModel
import org.koin.dsl.module

val timerActivityModule = module {
    factory { TimerActivityViewModel() }
    single { PermissionsManager }
}
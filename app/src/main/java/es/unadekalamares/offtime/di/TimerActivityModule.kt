package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.domain.permissions.PermissionsManager
import es.unadekalamares.offtime.ui.timer.TimerActivityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val timerActivityModule = module {
    viewModel { TimerActivityViewModel() }
    single { PermissionsManager }
}
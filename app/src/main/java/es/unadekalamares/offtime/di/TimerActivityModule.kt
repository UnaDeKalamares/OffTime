package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.ui.timer.TimerActivityViewModel
import org.koin.dsl.module

val timerModule = module {
    factory { TimerActivityViewModel() }
}
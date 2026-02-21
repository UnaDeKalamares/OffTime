package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.timer.TimerActivityViewModel
import org.koin.dsl.module

val timerModule = module {
    factory { TimerActivityViewModel() }
}
package es.unadekalamares.offtime

import android.app.Application
import es.unadekalamares.offtime.di.timerModule
import org.koin.core.context.startKoin

class TimerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(listOf(timerModule))
        }
    }

}
package es.unadekalamares.offtime

import android.app.Application
import es.unadekalamares.offtime.di.notificationsHelperModule
import es.unadekalamares.offtime.di.timerActivityModule
import es.unadekalamares.offtime.di.timerServiceModule
import es.unadekalamares.offtime.di.timerActivityViewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TimerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TimerApplication)
            modules(listOf(
                timerActivityModule,
                notificationsHelperModule,
                timerServiceModule,
                timerActivityViewModelModule)
            )
        }
    }

}
package es.unadekalamares.offtime.di

import es.unadekalamares.offtime.domain.datastore.DataStoreManager
import org.koin.dsl.module

val permissionsManagerModule = module {
    single { DataStoreManager }
}
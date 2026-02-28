package es.unadekalamares.offtime.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object DataStoreManager {

    val NOTIFICATION_PERMISSION_DENIED = booleanPreferencesKey("NOTIFICATION_PERMISSION_DENIED")

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

    fun isPermissionDenied(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[NOTIFICATION_PERMISSION_DENIED] ?: false
        }

    suspend fun setPermissionDenied(context: Context, isRequested: Boolean) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[NOTIFICATION_PERMISSION_DENIED] = isRequested
            }
        }
    }

}
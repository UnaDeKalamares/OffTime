package es.unadekalamares.offtime.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import es.unadekalamares.offtime.datastore.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object PermissionsManager: KoinComponent {

    private val dataStoreManager: DataStoreManager by inject()

    private val _permissionStateFlow: MutableStateFlow<PermissionStatus> = MutableStateFlow(
        PermissionStatus.Pending())
    val permissionStateFlow: StateFlow<PermissionStatus> = _permissionStateFlow.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun checkPermission(activity: Activity) {
        resetStateFlow()
        when (ContextCompat.checkSelfPermission(
            activity, Manifest.permission.POST_NOTIFICATIONS
        )) {
            PERMISSION_GRANTED -> _permissionStateFlow.value = PermissionStatus.Granted()
            else -> {
                dataStoreManager.isPermissionDenied(activity).collect { denied ->
                    if (denied) {
                        _permissionStateFlow.value = PermissionStatus.Denied()
                    } else {
                        _permissionStateFlow.value = checkPermissionRequest(activity)
                    }
                }
            }
        }

    }

    private fun checkPermissionRequest(activity: Activity): PermissionStatus =
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )) {
            PermissionStatus.RequestRationale()
        } else {
            PermissionStatus.Request()
        }

    suspend fun setPermissionAsDenied(activity: Activity) {
        dataStoreManager.setPermissionDenied(activity, true)
    }

    fun resetStateFlow() {
        _permissionStateFlow.value = PermissionStatus.Pending()
    }

}
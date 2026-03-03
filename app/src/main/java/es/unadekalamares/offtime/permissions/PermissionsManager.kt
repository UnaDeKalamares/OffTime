package es.unadekalamares.offtime.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import es.unadekalamares.offtime.datastore.DataStoreManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object PermissionsManager: KoinComponent {

    private val dataStoreManager: DataStoreManager by inject()

    private val _permissionSharedFlow: MutableSharedFlow<PermissionStatus> = MutableSharedFlow()
    val permissionSharedFlow: SharedFlow<PermissionStatus> = _permissionSharedFlow.asSharedFlow()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun checkPermission(activity: Activity) {
        when (ContextCompat.checkSelfPermission(
            activity, Manifest.permission.POST_NOTIFICATIONS
        )) {
            PERMISSION_GRANTED -> _permissionSharedFlow.emit(PermissionStatus.Granted())
            else -> {
                dataStoreManager.isPermissionDenied(activity).collect { denied ->
                    if (denied) {
                        _permissionSharedFlow.emit(PermissionStatus.Denied())
                    } else {
                        _permissionSharedFlow.emit(checkPermissionRequest(activity))
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun checkPermissionImmediate(activity: Activity): PermissionStatus {
        when (ContextCompat.checkSelfPermission(
            activity, Manifest.permission.POST_NOTIFICATIONS)) {
            PERMISSION_GRANTED -> return PermissionStatus.Granted()
            else -> {
                val denied = dataStoreManager.isPermissionDenied(activity).first()
                if (denied) {
                    return PermissionStatus.Denied()
                } else {
                    return checkPermissionRequest(activity)
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

}
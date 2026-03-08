package es.unadekalamares.offtime.domain.permissions

sealed class PermissionStatus {
    class Granted : PermissionStatus()
    class Denied: PermissionStatus()
    class Request: PermissionStatus()
    class RequestRationale: PermissionStatus()
}

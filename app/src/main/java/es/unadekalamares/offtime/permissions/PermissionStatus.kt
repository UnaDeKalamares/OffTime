package es.unadekalamares.offtime.permissions

sealed class PermissionStatus {
    class Pending: PermissionStatus()
    class Granted : PermissionStatus()
    class Denied: PermissionStatus()
    class Request: PermissionStatus()
    class RequestRationale: PermissionStatus()
}

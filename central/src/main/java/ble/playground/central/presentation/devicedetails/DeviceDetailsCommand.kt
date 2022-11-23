package ble.playground.central.presentation.devicedetails

sealed class DeviceDetailsCommand {
    data class RequestBluetoothPermission(val operation: Operation) : DeviceDetailsCommand()
}

enum class Operation {
    CONNECT, DISCONNECT
}

package ble.playground.central.presentation.device

sealed class DeviceDetailsCommand {
    data class RequestBluetoothPermission(val operation: Operation) : DeviceDetailsCommand()
}

enum class Operation {
    CONNECT, DISCONNECT
}

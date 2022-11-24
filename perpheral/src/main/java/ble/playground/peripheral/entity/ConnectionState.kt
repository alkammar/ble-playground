package ble.playground.peripheral.entity

sealed class ConnectionState {
    object Connected : ConnectionState()
    object NotConnected : ConnectionState()
}

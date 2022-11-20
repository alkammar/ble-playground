package ble.playground.central.entity

sealed class ConnectionState {
    object NotConnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Disconnecting : ConnectionState()
}

package ble.playground.perpheral.entity

sealed class ConnectionState {
    object Connected : ConnectionState()
    object NotConnected : ConnectionState()
}

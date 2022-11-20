package ble.playground.central.entity

data class Device(
    val id: String,
    val name: String,
    val connectionState: ConnectionState
)

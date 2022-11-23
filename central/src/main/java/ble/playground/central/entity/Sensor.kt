package ble.playground.central.entity

sealed class Sensor(open val id: String) {
    data class Available(override val id: String, val data: String) : Sensor(id)
    data class NotAvailable(override val id: String) : Sensor(id)
}

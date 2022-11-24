package ble.playground.peripheral.entity

sealed class AdvertisingState {
    object Advertising : AdvertisingState()
    object NotAdvertising : AdvertisingState()
}

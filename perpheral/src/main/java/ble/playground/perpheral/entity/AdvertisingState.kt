package ble.playground.perpheral.entity

sealed class AdvertisingState {
    object Advertising : AdvertisingState()
    object NotAdvertising : AdvertisingState()
}

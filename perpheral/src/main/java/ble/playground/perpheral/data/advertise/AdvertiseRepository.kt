package ble.playground.perpheral.data.advertise

interface AdvertiseRepository {
    fun startAdvertising(data: String)
}
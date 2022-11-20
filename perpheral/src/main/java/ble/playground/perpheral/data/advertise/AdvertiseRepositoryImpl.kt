package ble.playground.perpheral.data.advertise

import ble.playground.perpheral.datasource.ble.BlePeripheral

class AdvertiseRepositoryImpl(
    private val blePeripheral: BlePeripheral
) : AdvertiseRepository {

    override fun startAdvertising(data: String) {
        blePeripheral.startAdvertising(data)
    }
}
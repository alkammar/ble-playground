package ble.playground.perpheral.data.advertise

import ble.playground.perpheral.datasource.ble.BlePeripheral

class AdvertiseRepositoryImpl(
    private val blePeripheral: BlePeripheral
) : AdvertiseRepository {

    override fun data() = blePeripheral.advertiserFlow()

    override suspend fun startAdvertising() {
        blePeripheral.startAdvertising()
    }

    override suspend fun stopAdvertising() {
        blePeripheral.stopAdvertising()
    }

    override fun updateData(data: String) {
        blePeripheral.updateData(data)
    }
}
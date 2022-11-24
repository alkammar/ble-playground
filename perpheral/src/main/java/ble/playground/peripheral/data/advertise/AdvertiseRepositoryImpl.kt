package ble.playground.peripheral.data.advertise

import ble.playground.peripheral.datasource.ble.BleServer

class AdvertiseRepositoryImpl(
    private val bleServer: BleServer
) : AdvertiseRepository {

    override fun data() = bleServer.advertiserFlow()

    override suspend fun startAdvertising() {
        bleServer.startAdvertising()
    }

    override suspend fun stopAdvertising() {
        bleServer.stopAdvertising()
    }

    override fun updateData(data: String) {
        bleServer.updateData(data)
    }
}
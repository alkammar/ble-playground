package ble.playground.central.data.device.repository

import ble.playground.central.datasource.ble.BleClient

class DeviceRepositoryImpl(
    private val bleClient: BleClient
) : DeviceRepository {

    override suspend fun data() = bleClient.devicesFlow()

    override suspend fun connect(macAddress: String) {
        bleClient.connect(macAddress)
    }

    override suspend fun disconnect(macAddress: String) {
        bleClient.disconnect(macAddress)
    }
}
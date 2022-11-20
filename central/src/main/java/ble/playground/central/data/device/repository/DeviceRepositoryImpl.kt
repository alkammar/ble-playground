package ble.playground.central.data.device.repository

import ble.playground.central.datasource.ble.BleCentral

class DeviceRepositoryImpl(
    private val bleCentral: BleCentral
) : DeviceRepository {

    override suspend fun data() = bleCentral.devicesFlow()

    override suspend fun connect(macAddress: String) {
        bleCentral.connect(macAddress)
    }

    override suspend fun disconnect(macAddress: String) {
        bleCentral.disconnect(macAddress)
    }
}
package com.playground.ble.data.device.repository

import com.playground.ble.data.device.ble.BleService

class DeviceRepositoryImpl(
    private val bleService: BleService
) : DeviceRepository {

    override suspend fun data() = bleService.data()

    override suspend fun startScan() {
        bleService.startScan()
    }

    override suspend fun stopScan() {
        bleService.stopScan()
    }
}
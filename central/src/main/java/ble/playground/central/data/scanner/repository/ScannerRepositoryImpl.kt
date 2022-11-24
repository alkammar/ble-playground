package ble.playground.central.data.scanner.repository

import ble.playground.central.datasource.ble.BleClient

class ScannerRepositoryImpl(private val bleClient: BleClient) : ScannerRepository {

    override fun data() = bleClient.scannerFlow()

    override suspend fun startScan() {
        bleClient.startScan()
    }

    override suspend fun stopScan() {
        bleClient.stopScan()
    }
}
package ble.playground.central.data.scanner.repository

import ble.playground.central.datasource.ble.BleCentral

class ScannerRepositoryImpl(private val bleCentral: BleCentral) : ScannerRepository {

    override fun data() = bleCentral.scannerFlow()

    override suspend fun startScan() {
        bleCentral.startScan()
    }

    override suspend fun stopScan() {
        bleCentral.stopScan()
    }
}
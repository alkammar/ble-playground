package ble.playground.central.data.scanner.repository

import ble.playground.central.entity.Scanner
import kotlinx.coroutines.flow.Flow

interface ScannerRepository {
    fun data(): Flow<Scanner>
    suspend fun startScan()
    suspend fun stopScan()
}
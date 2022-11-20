package com.playground.ble.data.device.repository

import com.playground.ble.data.device.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    suspend fun data(): Flow<Set<Device>>
    suspend fun startScan()
    suspend fun stopScan()
}
package ble.playground.central.data.device.repository

import ble.playground.central.entity.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    suspend fun data(): Flow<Set<Device>>
    suspend fun connect(macAddress: String)
    suspend fun disconnect(macAddress: String)
}
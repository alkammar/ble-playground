package ble.playground.peripheral.data.advertise

import ble.playground.peripheral.entity.Advertiser
import kotlinx.coroutines.flow.Flow

interface AdvertiseRepository {
    fun data(): Flow<Advertiser>
    suspend fun startAdvertising()
    suspend fun stopAdvertising()
    fun updateData(data: String)
}
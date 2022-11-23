package ble.playground.perpheral.data.advertise

import ble.playground.perpheral.entity.Advertiser
import kotlinx.coroutines.flow.Flow

interface AdvertiseRepository {
    fun data(): Flow<Advertiser>
    suspend fun startAdvertising()
    fun updateData(data: String)
}
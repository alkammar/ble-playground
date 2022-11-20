package com.playground.ble.data.heartrate.repository

import com.playground.ble.data.heartrate.model.HeartRate
import kotlinx.coroutines.flow.Flow

interface HeartRateRepository {
    suspend fun data(): Flow<HeartRate>
}
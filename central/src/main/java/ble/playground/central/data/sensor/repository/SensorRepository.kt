package ble.playground.central.data.sensor.repository

import ble.playground.central.entity.Sensor
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun data(): Flow<Set<Sensor>>
}
package ble.playground.central.data.sensor.repository

import ble.playground.central.datasource.ble.BleCentral

class SensorRepositoryImpl(private val bleCentral: BleCentral) : SensorRepository {
    override fun data() = bleCentral.sensorsFlow()
}
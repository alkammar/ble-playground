package ble.playground.central.data.sensor.repository

import ble.playground.central.datasource.ble.BleClient

class SensorRepositoryImpl(private val bleClient: BleClient) : SensorRepository {
    override fun data() = bleClient.sensorsFlow()
}
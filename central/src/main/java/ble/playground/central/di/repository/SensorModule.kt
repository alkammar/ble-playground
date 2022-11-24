package ble.playground.central.di.repository

import ble.playground.central.datasource.ble.BleClient
import ble.playground.central.data.sensor.repository.SensorRepository
import ble.playground.central.data.sensor.repository.SensorRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class SensorModule {

    @Provides
    fun provideSensorRepository(bleClient: BleClient): SensorRepository =
        SensorRepositoryImpl(bleClient)
}
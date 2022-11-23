package ble.playground.central.di.device

import ble.playground.central.datasource.ble.BleCentral
import ble.playground.central.data.device.repository.DeviceRepository
import ble.playground.central.data.device.repository.DeviceRepositoryImpl
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
    fun provideSensorRepository(bleCentral: BleCentral): SensorRepository =
        SensorRepositoryImpl(bleCentral)
}
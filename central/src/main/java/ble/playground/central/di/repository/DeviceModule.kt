package ble.playground.central.di.repository

import ble.playground.central.datasource.ble.BleClient
import ble.playground.central.data.device.repository.DeviceRepository
import ble.playground.central.data.device.repository.DeviceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DeviceModule {

    @Provides
    fun provideDeviceRepository(bleClient: BleClient): DeviceRepository =
        DeviceRepositoryImpl(bleClient)
}
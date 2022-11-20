package ble.playground.central.di.device

import ble.playground.central.datasource.ble.BleCentral
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
    fun provideDeviceRepository(bleCentral: BleCentral): DeviceRepository =
        DeviceRepositoryImpl(bleCentral)
}
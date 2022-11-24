package ble.playground.peripheral.di.repository

import ble.playground.peripheral.data.advertise.AdvertiseRepository
import ble.playground.peripheral.data.advertise.AdvertiseRepositoryImpl
import ble.playground.peripheral.datasource.ble.BleServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AdvertiseModule {

    @Provides
    fun provideAdvertiseRepository(bleServer: BleServer): AdvertiseRepository =
        AdvertiseRepositoryImpl(bleServer)
}
package ble.playground.peripheral.di.advertise

import android.content.Context
import ble.playground.peripheral.data.advertise.AdvertiseRepository
import ble.playground.peripheral.data.advertise.AdvertiseRepositoryImpl
import ble.playground.peripheral.datasource.ble.BlePeripheral
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AdvertiseModule {

    @Provides
    fun provideAdvertiseRepository(blePeripheral: BlePeripheral): AdvertiseRepository =
        AdvertiseRepositoryImpl(blePeripheral)

    @Provides
    @Singleton
    fun provideBlePeripheral(@ApplicationContext applicationContext: Context) =
        BlePeripheral(applicationContext)
}
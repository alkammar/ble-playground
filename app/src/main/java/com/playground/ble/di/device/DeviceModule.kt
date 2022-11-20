package com.playground.ble.di.device

import android.content.Context
import com.playground.ble.data.device.ble.BleService
import com.playground.ble.data.device.repository.DeviceRepository
import com.playground.ble.data.device.repository.DeviceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DeviceModule {

    @Provides
    fun provideDeviceRepository(bleService: BleService): DeviceRepository =
        DeviceRepositoryImpl(bleService)

    @Provides
    @Singleton
    fun provideBleService(@ApplicationContext applicationContext: Context) =
        BleService(applicationContext)
}
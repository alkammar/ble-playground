package ble.playground.peripheral.di.datasource

import android.content.Context
import ble.playground.peripheral.datasource.ble.BleServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class BleModule {

    @Provides
    @Singleton
    fun provideBleServer(@ApplicationContext applicationContext: Context) =
        BleServer(applicationContext)
}
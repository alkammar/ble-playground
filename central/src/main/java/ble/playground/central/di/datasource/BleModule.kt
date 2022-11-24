package ble.playground.central.di.datasource

import android.content.Context
import ble.playground.central.datasource.ble.BleClient
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
    fun provideBleClient(@ApplicationContext applicationContext: Context) =
        BleClient(applicationContext)
}
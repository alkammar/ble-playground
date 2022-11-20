package ble.playground.central.di.scanner

import ble.playground.central.datasource.ble.BleCentral
import ble.playground.central.data.scanner.repository.ScannerRepository
import ble.playground.central.data.scanner.repository.ScannerRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ScannerModule {

    @Provides
    fun provideScannerRepository(bleCentral: BleCentral): ScannerRepository =
        ScannerRepositoryImpl(bleCentral)
}
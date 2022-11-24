package ble.playground.peripheral.presentation.advertise

sealed class AdvertiseCommand {
    object RequestBluetoothPermission : AdvertiseCommand()
    object RequestLocationPermission : AdvertiseCommand()
}

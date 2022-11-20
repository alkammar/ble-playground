package ble.playground.perpheral.presentation.advertise

sealed class AdvertiseCommand {
    object RequestBluetoothPermission : AdvertiseCommand()
    object RequestLocationPermission : AdvertiseCommand()
}

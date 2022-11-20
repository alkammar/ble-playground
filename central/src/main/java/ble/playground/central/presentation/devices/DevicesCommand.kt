package ble.playground.central.presentation.devices

sealed class DevicesCommand {
    object RequestBluetoothPermission : DevicesCommand()
    object RequestLocationPermission : DevicesCommand()
}

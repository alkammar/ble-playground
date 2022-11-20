package com.playground.ble.presentation.devices

sealed class DevicesCommand {
    object RequestBluetoothPermission : DevicesCommand()
    object RequestLocationPermission : DevicesCommand()
}

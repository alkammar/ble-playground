package ble.playground.central.presentation.scanner

sealed class ScannerCommand {
    object RequestBluetoothPermission : ScannerCommand()
    object RequestLocationPermission : ScannerCommand()
}

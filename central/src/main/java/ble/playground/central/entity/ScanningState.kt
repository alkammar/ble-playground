package ble.playground.central.entity

sealed class ScanningState {
    data class Scanning(val expiresAtMillisecond: Long) : ScanningState()
    object NotScanning : ScanningState()
}

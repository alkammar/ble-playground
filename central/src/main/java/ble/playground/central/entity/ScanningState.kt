package ble.playground.central.entity

sealed class ScanningState {
    data class Scanning(val remainingTimeMillisecond: Long) : ScanningState()
    object NotScanning : ScanningState()
}

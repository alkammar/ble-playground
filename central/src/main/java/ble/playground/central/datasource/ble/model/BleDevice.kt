package ble.playground.central.datasource.ble.model

import android.bluetooth.BluetoothGatt
import ble.playground.central.entity.ConnectionState

data class BleDevice(
    val macAddress: String,
    val name: String,
    val gatt: BluetoothGatt? = null,
    val connectionState: ConnectionState
)

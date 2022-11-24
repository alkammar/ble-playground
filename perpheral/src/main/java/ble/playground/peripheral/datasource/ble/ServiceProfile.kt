package ble.playground.peripheral.datasource.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattDescriptor.PERMISSION_READ
import android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import java.util.*

class ServiceProfile {

    companion object {
        val BLE_SERVICE_UUID: UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
        val BLE_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        fun createBleService(): BluetoothGattService {
            val service = BluetoothGattService(
                BLE_SERVICE_UUID,
                SERVICE_TYPE_PRIMARY
            )

            val currentTime = BluetoothGattCharacteristic(
                BLE_CHARACTERISTIC_UUID,
                PROPERTY_READ or PROPERTY_NOTIFY,
                PERMISSION_READ
            )
            val configDescriptor = BluetoothGattDescriptor(
                CLIENT_CONFIG,
                PERMISSION_READ or PERMISSION_WRITE
            )
            currentTime.addDescriptor(configDescriptor)

            service.addCharacteristic(currentTime)

            return service
        }
    }
}
package ble.playground.central.datasource.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.util.Log
import ble.playground.central.datasource.ble.model.BleDevice
import ble.playground.central.entity.ConnectionState
import ble.playground.central.entity.Sensor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.util.*

private const val LOG_TAG = "BLE Playground"

class BleGattCallback(
    private val bleDevice: BleDevice,
    private val devicesFlow: MutableSharedFlow<Set<BleDevice>>,
    private val sensorsFlow: MutableSharedFlow<Set<Sensor>>
) : BluetoothGattCallback() {
    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int
    ) {
        handleConnectionStateChanged(status, newState, gatt)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        handleServiceDiscovered(status, gatt)
    }

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        handleCharacteristicReadLegacy(characteristic)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        handleCharacteristicRead(characteristic, value)
    }

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        handleCharacteristicReadLegacy(characteristic)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        handleCharacteristicRead(characteristic, value)
    }

    private fun handleCharacteristicRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        runBlocking {
            sensorsFlow.updateAndEmit(
                Sensor.Available(
                    characteristic.uuid.toString(),
                    String(value)
                )
            )
        }
    }

    private fun handleCharacteristicReadLegacy(characteristic: BluetoothGattCharacteristic?) {
        runBlocking {
            characteristic?.let { characteristic ->
                characteristic.value?.let { value ->
                    sensorsFlow.updateAndEmit(
                        Sensor.Available(
                            characteristic.uuid.toString(),
                            String(value)
                        )
                    )
                } ?: sensorsFlow.updateAndEmit(
                    Sensor.NotAvailable(
                        characteristic.uuid.toString()
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleServiceDiscovered(status: Int, gatt: BluetoothGatt?) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            gatt?.getService(UUID.fromString(ServiceProfile.BLE_SERVICE_UUID))
                ?.let { service ->
                    Log.d(LOG_TAG, "Discovered service ${service.uuid}")
                    service.getCharacteristic(UUID.fromString(ServiceProfile.BLE_CHARACTERISTIC_UUID))
                        ?.let { characteristic ->
                            Log.d(
                                LOG_TAG,
                                "Discovered characteristic ${characteristic.uuid}"
                            )
                            requestNotification(gatt, characteristic)
                        }
                }
        } else {
            gatt?.disconnect()
            Log.d(LOG_TAG, "Error discovering services $status")
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNotification(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
    ) {
        val descriptor: BluetoothGattDescriptor =
            characteristic.getDescriptor(UUID.fromString(ServiceProfile.CLIENT_CONFIG))
        if (gatt.setCharacteristicNotification(characteristic, true)) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (!gatt.writeDescriptor(descriptor)) {
                Log.e(
                    LOG_TAG,
                    "Failed to write characteristic to enable notification ${characteristic.uuid}"
                )
            }
        } else {
            Log.e(LOG_TAG, "Failed to request characteristic notification ${characteristic.uuid}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnectionStateChanged(
        status: Int,
        newState: Int,
        gatt: BluetoothGatt?
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runBlocking {
                    devicesFlow.updateAndEmit(bleDevice.copy(connectionState = ConnectionState.Connected))
                }
                gatt?.discoverServices()
                Log.d(
                    LOG_TAG,
                    "Connected to ${gatt?.device?.name} ${gatt?.device?.address}"
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runBlocking {
                    devicesFlow.updateAndEmit(bleDevice.copy(connectionState = ConnectionState.NotConnected))
                }
                gatt?.close()
                Log.d(LOG_TAG, "Disconnected from ${gatt?.device?.name} ${gatt?.device?.address}")
            }
        } else {
            gatt?.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun readCharacteristic(
        bluetoothGatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic?
    ) = characteristic?.let {
        if (it.isReadable()) {
            bluetoothGatt.readCharacteristic(characteristic)
        } else {
            false
        }
    } ?: false

    private fun BluetoothGattCharacteristic.isReadable(): Boolean = containsProperty(
        BluetoothGattCharacteristic.PROPERTY_READ
    )

    private fun BluetoothGattCharacteristic.containsProperty(property: Int) =
        properties and property != 0
}
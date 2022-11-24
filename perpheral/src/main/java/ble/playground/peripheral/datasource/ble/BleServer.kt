package ble.playground.peripheral.datasource.ble

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_FAILURE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
import android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat.checkSelfPermission
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import ble.playground.peripheral.datasource.ble.ServiceProfile.Companion.BLE_CHARACTERISTIC_UUID
import ble.playground.peripheral.datasource.ble.ServiceProfile.Companion.BLE_SERVICE_UUID
import ble.playground.peripheral.datasource.ble.ServiceProfile.Companion.CLIENT_CONFIG
import ble.playground.peripheral.entity.Advertiser
import ble.playground.peripheral.entity.AdvertisingState.Advertising
import ble.playground.peripheral.entity.AdvertisingState.NotAdvertising
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.*

private const val LOG_TAG = "BLE Playground"

class BleServer(
    private val context: Context
) {

    private var data: String = "22"
    private var gattServerCallback: GattServerCallback? = null
    private var gattServer: BluetoothGattServer? = null
    private val subscribedDevices = mutableMapOf<String, BluetoothDevice>()

    private val advertiserFlow = MutableSharedFlow<Advertiser>(replay = 1)

    fun advertiserFlow() = advertiserFlow

    private val bluetoothManager by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    }

    init {
        GlobalScope.launch {
            advertiserFlow.emit(Advertiser(NotAdvertising))
        }
    }

    suspend fun startAdvertising() {
        if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            setupGattServer()
            startAdvertisingInternal()
            advertiserFlow.emit(Advertiser(Advertising))
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun stopAdvertising() {
        if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            bluetoothManager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertisingCallback)
            advertiserFlow.emit(Advertiser(NotAdvertising))
            Log.d(LOG_TAG, "Advertising stopped")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertisingInternal() {
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(BLE_SERVICE_UUID))
            .build()

        val advertiser = bluetoothManager.adapter.bluetoothLeAdvertiser
        advertiser.startAdvertising(advertiseSettings, advertiseData, advertisingCallback)
    }

    private val advertisingCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(LOG_TAG, "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d(LOG_TAG, "Advertising failed")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        if (gattServerCallback == null) {
            gattServerCallback = GattServerCallback()

            gattServer = bluetoothManager.openGattServer(context, gattServerCallback).apply {
                addService(ServiceProfile.createBleService())
            }
        }
    }

    private inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (status == GATT_SUCCESS && newState == STATE_CONNECTED) {
                Log.d(LOG_TAG, "Connected to ${device.address}")
            } else {
                subscribedDevices.remove(device.address)
                Log.d(LOG_TAG, "Disconnected from ${device.address}")
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)

            Log.d(LOG_TAG, "Notification sent to ${device?.address}")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.d(LOG_TAG, "Characteristic ${characteristic?.uuid} read request from ${device?.address}")
            gattServer?.sendResponse(
                device,
                requestId,
                GATT_SUCCESS,
                0,
                data.toByteArray(Charset.forName("UTF-8"))
            )
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            Log.d(LOG_TAG, "Characteristic ${characteristic?.uuid} write request from ${device?.address}")
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice, requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean, responseNeeded: Boolean,
            offset: Int, value: ByteArray
        ) {
            if (CLIENT_CONFIG == descriptor.uuid) {
                if (Arrays.equals(ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(LOG_TAG, "Subscribe device to notifications: $device")
                    subscribedDevices[device.address] = device
                } else if (Arrays.equals(DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(LOG_TAG, "Unsubscribe device from notifications: $device")
                    subscribedDevices.remove(device.address)
                }

                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        GATT_SUCCESS,
                        0,
                        null
                    )
                }
            } else {
                Log.w(LOG_TAG, "Unknown descriptor write request")
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        GATT_FAILURE,
                        0,
                        null
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            if (CLIENT_CONFIG == descriptor.uuid) {
                Log.d(LOG_TAG, "Config descriptor read")
                val returnValue = if (subscribedDevices.contains(device.address)) {
                    ENABLE_NOTIFICATION_VALUE
                } else {
                    DISABLE_NOTIFICATION_VALUE
                }
                gattServer?.sendResponse(
                    device,
                    requestId,
                    GATT_SUCCESS,
                    0,
                    returnValue
                )
            } else {
                Log.w(LOG_TAG, "Unknown descriptor read request")
                gattServer?.sendResponse(
                    device,
                    requestId,
                    GATT_FAILURE,
                    0,
                    null
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun updateData(data: String) {
        this.data = data

        val characteristic = gattServer
            ?.getService(BLE_SERVICE_UUID)
            ?.getCharacteristic(BLE_CHARACTERISTIC_UUID)
        characteristic?.value = data.toByteArray(Charset.forName("UTF-8"))

        subscribedDevices.forEach {
            gattServer?.notifyCharacteristicChanged(
                it.value,
                characteristic,
                false
            )
        }
    }

    private fun isBluetoothPermissionGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(context, BLUETOOTH_ADVERTISE) == PERMISSION_GRANTED &&
                    checkSelfPermission(context, BLUETOOTH_CONNECT) == PERMISSION_GRANTED
        } else {
            checkSelfPermission(context, BLUETOOTH_ADMIN) == PERMISSION_GRANTED
        }
}
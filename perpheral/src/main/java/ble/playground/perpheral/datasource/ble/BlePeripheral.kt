package ble.playground.perpheral.datasource.ble

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat.checkSelfPermission
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import ble.playground.perpheral.entity.Advertiser
import ble.playground.perpheral.entity.AdvertisingState.Advertising
import ble.playground.perpheral.entity.AdvertisingState.NotAdvertising
import kotlinx.coroutines.flow.MutableSharedFlow
import java.nio.charset.Charset
import java.util.*

private const val SERVICE_UUID = "ddd6c04c-3fe6-4723-b2d5-f67c4cf4456a"
private const val VALUE_UUID = "ddd6c04c-3fe6-4723-b2d5-f67c4cf4456b"

class BlePeripheral(
    private val context: Context
) {

    private var data: String = "22"
    private lateinit var gattServerCallback: GattServerCallback
    private lateinit var gattServer: BluetoothGattServer
    private var bluetoothDevice: BluetoothDevice? = null

    private val advertiserFlow = MutableSharedFlow<Advertiser>(replay = 1)

    fun advertiserFlow() = advertiserFlow

    private val bluetoothManager by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    }

    suspend fun startAdvertising(data: String) {
        if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            advertiserFlow.emit(Advertiser(NotAdvertising))
            startAdvertisingInternal(data)
            advertiserFlow.emit(Advertiser(Advertising))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertisingInternal(data: String) {
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val advertiseData = AdvertiseData.Builder()
//            .setIncludeDeviceName(true)
//                .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
            .addServiceData(
                ParcelUuid(UUID.fromString(SERVICE_UUID)),
                data.toByteArray(Charset.forName("UTF-8"))
            )
            .build()

        val advertiser = bluetoothManager.adapter.bluetoothLeAdvertiser
//        advertiser.stopAdvertising(advertisingCallback)
        advertiser.startAdvertising(advertiseSettings, advertiseData, advertisingCallback)
    }

    private val advertisingCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            println("kammer ??? onStartSuccess")

            setupGattServer()
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            println("kammer ??? onStartFailure $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        gattServerCallback = GattServerCallback()

        gattServer = bluetoothManager.openGattServer(context, gattServerCallback).apply {
            addService(setupGattService())
        }
    }

    private fun setupGattService(): BluetoothGattService {
        val service = BluetoothGattService(
            UUID.fromString(SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val messageCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(VALUE_UUID),
            PROPERTY_NOTIFY,
            PERMISSION_READ
        )
        service.addCharacteristic(messageCharacteristic)
        return service
    }

    private inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            val isSuccess = status == GATT_SUCCESS
            val isConnected = newState == STATE_CONNECTED
            if (isSuccess && isConnected) {
                bluetoothDevice = device
//                setCurrentChatConnection(device)
            } else {
//                _deviceConnection.postValue(DeviceConnectionState.Disconnected)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)

            println("kammer ??? onNotificationSent to ${device?.address}")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            println("kammer ??? onCharacteristicReadRequest from ${device?.address} characteristic ${characteristic?.uuid}")
            gattServer.sendResponse(
                device,
                requestId,
                GATT_SUCCESS,
                0,
                data.toByteArray(Charset.forName("UTF-8"))
//                byteArrayOf()
            ).also {
                if (it) {
                    println("kammer ??? response ${"77"} sent to ${device?.address}")
                }
            }
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
            println("kammer ??? onCharacteristicWriteRequest from ${device?.address} characteristic ${characteristic?.uuid}")
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            println("kammer ??? onCharacteristicWriteRequest from ${device?.address} characteristic ${descriptor?.uuid}")
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
package ble.playground.perpheral.datasource.ble

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat.checkSelfPermission
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import java.nio.charset.Charset
import java.util.*

private const val SERVICE_UUID = "ddd6c04c-3fe6-4723-b2d5-f67c4cf4456a"

class BlePeripheral(
    private val context: Context
) {

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    fun startAdvertising(data: String) {
        if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            startAdvertisingInternal(data)
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
            .addServiceData(ParcelUuid(UUID.fromString(SERVICE_UUID)), data.toByteArray(Charset.forName("UTF-8")))
            .build()

        val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        advertiser.stopAdvertising(advertisingCallback)
        advertiser.startAdvertising(advertiseSettings, advertiseData, advertisingCallback)
    }

    private val advertisingCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            println("kammar onStartSuccess")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            println("kammar onStartFailure $errorCode")
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
package com.playground.ble.data.device.ble

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.app.ActivityCompat.checkSelfPermission
import com.playground.ble.data.device.model.Device
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BleService(
    private val context: Context
) {

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val flow = MutableSharedFlow<Set<Device>>(replay = 1)

    init {
        GlobalScope.launch {
            flow.emit(emptySet())
        }
    }

    fun data() = flow

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!isLocationPermissionGranted()) {
            throw LocationPermissionNotGrantedException()
        } else if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isLocationPermissionGranted()) {
            throw LocationPermissionNotGrantedException()
        } else if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            runBlocking {
                with(flow.first().toMutableSet()) {
                    add(Device(id = result.device.address, name = result.device.name ?: ""))
                    flow.emit(this)
                }
            }
        }
    }

    private fun isLocationPermissionGranted() =
        checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

    private fun isBluetoothPermissionGranted() =
        checkSelfPermission(context, BLUETOOTH_ADMIN) == PERMISSION_GRANTED
}
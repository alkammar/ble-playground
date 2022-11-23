package ble.playground.central.datasource.ble

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat.checkSelfPermission
import ble.playground.central.datasource.ble.model.BleDevice
import ble.playground.central.entity.ConnectionState.*
import ble.playground.central.entity.Device
import ble.playground.central.entity.Scanner
import ble.playground.central.entity.ScanningState.NotScanning
import ble.playground.central.entity.ScanningState.Scanning
import ble.playground.central.entity.Sensor
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import ble.playground.common.data.DeviceNotFoundException
import ble.playground.common.data.LocationPermissionNotGrantedException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*

private const val SCANNING_PERIOD = 60_000L

private const val LOG_TAG = "BLE Playground"

class BleCentral(
    private val context: Context
) {
    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val scannerFlow = MutableSharedFlow<Scanner>(replay = 1)
    private val devicesFlow = MutableSharedFlow<Set<BleDevice>>(replay = 1)
    private val sensorsFlow = MutableSharedFlow<Set<Sensor>>(replay = 1)

    private var timerJob: Job? = null

    init {
        GlobalScope.launch {
            scannerFlow.emit(Scanner(NotScanning))
            devicesFlow.emit(emptySet())
            sensorsFlow.emit(emptySet())
        }
    }

    fun scannerFlow() = scannerFlow

    fun devicesFlow() = devicesFlow.map { bleDevices ->
        bleDevices.map {
            Device(
                it.macAddress,
                it.name,
                it.connectionState
            )
        }.toSet()
    }

    fun sensorsFlow() = sensorsFlow

    @SuppressLint("MissingPermission")
    suspend fun startScan() {
        if (!isLocationPermissionGranted()) {
            throw LocationPermissionNotGrantedException()
        } else if (!isScanBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            scannerFlow.emit(Scanner(Scanning(Calendar.getInstance().timeInMillis + SCANNING_PERIOD)))
            bluetoothAdapter.bluetoothLeScanner.startScan(
                buildScanFilter(),
                ScanSettings.Builder().build(),
                scanCallback
            )

            timerJob = GlobalScope.launch {
                delay(SCANNING_PERIOD)
                scannerFlow.emit(Scanner(NotScanning))
                bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
            }
        }
    }

    private fun buildScanFilter() = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(ServiceProfile.BLE_SERVICE_UUID)))
            .build()
    )

    @SuppressLint("MissingPermission")
    suspend fun stopScan() {
        if (!isLocationPermissionGranted()) {
            throw LocationPermissionNotGrantedException()
        } else if (!isScanBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            timerJob?.cancel()
            scannerFlow.emit(Scanner(NotScanning))
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            runBlocking {
                devicesFlow.addAndEmit(
                    BleDevice(
                        macAddress = result.device.address,
                        name = result.device.name.orEmpty(),
                        connectionState = NotConnected
                    )
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runBlocking {
                if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
                    Log.w(LOG_TAG, "Scanning already in progress")
                } else {
                    timerJob?.cancel()
                    scannerFlow.emit(Scanner(NotScanning))
                    Log.e(LOG_TAG, "Error scanning $errorCode")
                }
            }
        }
    }

    suspend fun connect(macAddress: String) {
        with(devicesFlow.first().toMutableSet()) {
            firstOrNull { it.macAddress == macAddress }?.let { bleDevice ->
                try {
                    devicesFlow.updateAndEmit(bleDevice.copy(connectionState = Connecting))
                    connectGatt(bleDevice)
                } catch (exception: IllegalArgumentException) {
                    devicesFlow.removeAndEmit(bleDevice)
                    throw DeviceNotFoundException()
                }
            } ?: throw DeviceNotFoundException()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectGatt(bleDevice: BleDevice) =
        bluetoothAdapter
            .getRemoteDevice(bleDevice.macAddress)
            .connectGatt(
                context,
                false,
                BleGattCallback(bleDevice, devicesFlow, sensorsFlow),
                TRANSPORT_LE
            )

    @SuppressLint("MissingPermission")
    suspend fun disconnect(macAddress: String) {
        if (!isConnectBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            with(devicesFlow.first().toMutableSet()) {
                firstOrNull { it.macAddress == macAddress }?.let { bleDevice ->
                    devicesFlow.updateAndEmit(bleDevice.copy(connectionState = Disconnecting))
                    bleDevice.gatt?.disconnect()
                    devicesFlow.updateAndEmit(bleDevice.copy(connectionState = NotConnected))
                } ?: throw DeviceNotFoundException()
            }
        }
    }

    private fun isLocationPermissionGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            true
        } else {
            checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                    checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
        }

    private fun isScanBluetoothPermissionGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(context, BLUETOOTH_SCAN) == PERMISSION_GRANTED
        } else {
            true
        }

    private fun isConnectBluetoothPermissionGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(context, BLUETOOTH_CONNECT) == PERMISSION_GRANTED
        } else {
            true
        }
}
package ble.playground.central.datasource.ble

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat.checkSelfPermission
import ble.playground.central.data.device.model.BleDevice
import ble.playground.central.entity.ConnectionState.*
import ble.playground.central.entity.Device
import ble.playground.central.entity.Scanner
import ble.playground.central.entity.ScanningState.NotScanning
import ble.playground.central.entity.ScanningState.Scanning
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import ble.playground.common.data.DeviceNotFoundException
import ble.playground.common.data.LocationPermissionNotGrantedException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

private const val SERVICE_UUID = "ddd6c04c-3fe6-4723-b2d5-f67c4cf4456a"

class BleCentral(
    private val context: Context
) {

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val scannerFlow = MutableSharedFlow<Scanner>(replay = 1)
    private val devicesFlow = MutableSharedFlow<Set<BleDevice>>(replay = 1)

    init {
        GlobalScope.launch {
            scannerFlow.emit(Scanner(NotScanning))
            devicesFlow.emit(emptySet())
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

    @SuppressLint("MissingPermission")
    suspend fun startScan() {
        if (!isLocationPermissionGranted()) {
            throw LocationPermissionNotGrantedException()
        } else if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
            scannerFlow.emit(Scanner(Scanning(60)))
            bluetoothAdapter.bluetoothLeScanner.startScan(
                buildScanFilter(),
                ScanSettings.Builder().build(),
                scanCallback
            )
        }
    }

    private fun buildScanFilter() = listOf(
        ScanFilter.Builder()
            .setServiceData(ParcelUuid(UUID.fromString(SERVICE_UUID)), byteArrayOf())
            .build()
    )

    @SuppressLint("MissingPermission")
    suspend fun stopScan() {
        if (!isLocationPermissionGranted()) {
            throw LocationPermissionNotGrantedException()
        } else if (!isBluetoothPermissionGranted()) {
            throw BluetoothPermissionNotGrantedException()
        } else {
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
                        name = result.device.name ?: "",
                        connectionState = NotConnected
                    )
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runBlocking {
                scannerFlow.emit(Scanner(NotScanning))
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
            .connectGatt(context, false, object : BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt?,
                    status: Int,
                    newState: Int
                ) {
                    if (status == GATT_SUCCESS) {
                        if (newState == STATE_CONNECTED) {
                            println("kammer ??? connected to ${gatt?.device?.address}")
                            println("kammer ??? connected to ${gatt?.device?.name}")

                            runBlocking {
                                devicesFlow.updateAndEmit(bleDevice.copy(connectionState = Connected))
                            }
                            gatt?.discoverServices()
                        } else if (newState == STATE_DISCONNECTED) {
                            println("kammer ??? disconnected from ${gatt?.device?.address}")
                            runBlocking {
                                devicesFlow.updateAndEmit(bleDevice.copy(connectionState = NotConnected))
                            }
                            gatt?.close()
                        }
                    } else {
                        gatt?.close()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if (status == GATT_SUCCESS) {
                        gatt?.services?.forEach {
                            it.characteristics.forEach {
                                it.descriptors.forEach {
                                    println("kammer ??? discovered ${it} from ${bleDevice.macAddress}")
                                }
                            }
                        }
                    } else {
                        gatt?.disconnect()
                        println("kammer ??? no gatt from ${gatt?.device?.address}")
                    }
                }
            }, TRANSPORT_LE)

    @SuppressLint("MissingPermission")
    suspend fun disconnect(macAddress: String) {
        with(devicesFlow.first().toMutableSet()) {
            firstOrNull { it.macAddress == macAddress }?.let { bleDevice ->
                devicesFlow.updateAndEmit(bleDevice.copy(connectionState = Disconnecting))
                bleDevice.gatt?.disconnect()
                devicesFlow.updateAndEmit(bleDevice.copy(connectionState = NotConnected))
            } ?: throw DeviceNotFoundException()
        }
    }

    private fun isLocationPermissionGranted() =
        checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED &&
                checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

    private fun isBluetoothPermissionGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(context, BLUETOOTH_SCAN) == PERMISSION_GRANTED
        } else {
            checkSelfPermission(context, BLUETOOTH_ADMIN) == PERMISSION_GRANTED
        }

    private suspend fun Flow<Set<BleDevice>>.addAndEmit(bleDevice: BleDevice) {
        with(first().toMutableSet()) {
            add(bleDevice)
            devicesFlow.emit(this)
        }
    }

    private suspend fun Flow<Set<BleDevice>>.removeAndEmit(bleDevice: BleDevice) {
        with(first().toMutableSet()) {
            removeIf { it.macAddress == bleDevice.macAddress }
            devicesFlow.emit(this)
        }
    }

    private suspend fun Flow<Set<BleDevice>>.updateAndEmit(bleDevice: BleDevice) {
        with(first().toMutableSet()) {
            replace(bleDevice)
            devicesFlow.emit(this)
        }
    }

    private fun MutableSet<BleDevice>.replace(bleDevice: BleDevice) {
        removeIf { it.macAddress == bleDevice.macAddress }
        add(bleDevice)
    }
}
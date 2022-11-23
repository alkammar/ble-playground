package ble.playground.central.datasource.ble

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*


private const val SERVICE_UUID = "00001805-0000-1000-8000-00805f9b34fb"
private const val VALUE_UUID = "00002a2b-0000-1000-8000-00805f9b34fb"
private const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

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
        } else if (!isBluetoothPermissionGranted()) {
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
            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
            .build()
    )

    @SuppressLint("MissingPermission")
    suspend fun stopScan() {
        if (!isLocationPermissionGranted()) {
            throw LocationPermissionNotGrantedException()
        } else if (!isBluetoothPermissionGranted()) {
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
                println("kammer error $errorCode")
                if (errorCode != SCAN_FAILED_ALREADY_STARTED) {
                    timerJob?.cancel()
                    scannerFlow.emit(Scanner(NotScanning))
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
                    println("kammer ??? onServicesDiscovered")
                    if (status == GATT_SUCCESS) {
                        gatt?.getService(UUID.fromString(SERVICE_UUID))
                            ?.getCharacteristic(UUID.fromString(VALUE_UUID))?.let {
                                println("kammer ??? characteristic ${it.uuid} from ${bleDevice.macAddress}")
                                requestNotification(gatt, it)
//                                if (!readCharacteristic(gatt, it)) {
//                                    Log.w(LOG_TAG, "Unable to read characteristic ${it.uuid}")
//                                }
                            }
                    } else {
                        gatt?.disconnect()
                        println("kammer ??? no gatt from ${gatt?.device?.address}")
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?
                ) {
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

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
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

                @Deprecated("Deprecated in Java")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
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

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int
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
            }, TRANSPORT_LE)

    @SuppressLint("MissingPermission")
    private fun requestNotification(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
    ) {
        val descriptor: BluetoothGattDescriptor =
            characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID))
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
    private fun readCharacteristic(
        bluetoothGatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic?
    ) =
        characteristic?.let {
            if (it.isReadable()) {
                bluetoothGatt.readCharacteristic(characteristic)
            } else {
                false
            }
        } ?: false

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

    private suspend fun Flow<Set<Sensor>>.updateAndEmit(sensor: Sensor) {
        with(first().toMutableSet()) {
            replace(sensor)
            sensorsFlow.emit(this)
        }
    }

    private fun MutableSet<BleDevice>.replace(bleDevice: BleDevice) {
        removeIf { it.macAddress == bleDevice.macAddress }
        add(bleDevice)
    }

    private fun MutableSet<Sensor>.replace(sensor: Sensor) {
        removeIf { it.id == sensor.id }
        add(sensor)
    }

    private fun BluetoothGattCharacteristic.isReadable(): Boolean = containsProperty(PROPERTY_READ)

    private fun BluetoothGattCharacteristic.isWritable(): Boolean = containsProperty(PROPERTY_WRITE)

    private fun BluetoothGattCharacteristic.containsProperty(property: Int) =
        properties and property != 0
}
package ble.playground.central.presentation.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ble.playground.central.entity.Device
import ble.playground.central.data.device.repository.DeviceRepository
import ble.playground.central.data.sensor.repository.SensorRepository
import ble.playground.central.entity.Sensor
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import ble.playground.common.presentation.SingleLiveEvent
import ble.playground.common.presentation.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    val device: LiveData<State<Device>> get() = _device
    private val _device = MutableLiveData<State<Device>>()

    val sensor: LiveData<State<Sensor>> get() = _sensor
    private val _sensor = MutableLiveData<State<Sensor>>()

    val notification = SingleLiveEvent<DeviceDetailsCommand>()

    init {
        _device.value = State.empty()
    }

    fun onViewCreated(deviceMacAddress: String) {
        viewModelScope.launch {
            deviceRepository.data()
                .map { devices -> devices.first { it.id == deviceMacAddress } }
                .collect {
                    _device.value = State.success(it)
                }
        }

        viewModelScope.launch {
            sensorRepository.data()
                .map { sensors -> sensors.firstOrNull() }
                .collect { sensor ->
                    sensor?.let { _sensor.value = State.success(it) }
                }
        }
    }

    fun onConnectAction(deviceMacAddress: String) {
        viewModelScope.launch {
            try {
                deviceRepository.connect(deviceMacAddress)
            } catch (e: BluetoothPermissionNotGrantedException) {
                notification.value = DeviceDetailsCommand.RequestBluetoothPermission(Operation.CONNECT)
            }
        }
    }

    fun onDisconnectAction(deviceMacAddress: String) {
        viewModelScope.launch {
            try {
                deviceRepository.disconnect(deviceMacAddress)
            } catch (e: BluetoothPermissionNotGrantedException) {
                notification.value = DeviceDetailsCommand.RequestBluetoothPermission(Operation.DISCONNECT)
            }
        }
    }

    fun onBluetoothScanPermissionGranted(operation: Operation, deviceMacAddress: String) {
        when(operation) {
            Operation.CONNECT -> onConnectAction(deviceMacAddress)
            Operation.DISCONNECT -> onDisconnectAction(deviceMacAddress)
        }
    }

    fun onBluetoothScanPermissionDenied() {
        // Explain to the user that the feature is unavailable because the
        // feature requires a permission that the user has denied. At the
        // same time, respect the user's decision. Don't link to system
        // settings in an effort to convince the user to change their
        // decision.
    }
}
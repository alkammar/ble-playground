package ble.playground.central.presentation.devicedetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ble.playground.central.entity.Device
import ble.playground.central.data.device.repository.DeviceRepository
import ble.playground.central.data.sensor.repository.SensorRepository
import ble.playground.central.entity.Sensor
import ble.playground.common.presentation.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    val device: LiveData<State<Device>> get() = _device
    private val _device = MutableLiveData<State<Device>>()

    val sensor: LiveData<State<Sensor>> get() = _sensor
    private val _sensor = MutableLiveData<State<Sensor>>()

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
            deviceRepository.connect(deviceMacAddress)
        }
    }

    fun onDisconnectAction(deviceMacAddress: String) {
        viewModelScope.launch {
            deviceRepository.disconnect(deviceMacAddress)
        }
    }
}
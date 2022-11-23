package ble.playground.central.presentation.devicedetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ble.playground.central.entity.Device
import ble.playground.central.data.device.repository.DeviceRepository
import ble.playground.common.presentation.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    val device: LiveData<State<Device>> get() = _device
    private val _device = MutableLiveData<State<Device>>()

    init {
        _device.value = State.empty()
    }

    fun onViewCreated(deviceMacAddress: String) {
        viewModelScope.launch {
            repository.data()
                .map { devices -> devices.first { it.id == deviceMacAddress } }
                .collect {
                    _device.value = State.success(it)
                }
        }
    }

    fun onConnectAction(deviceMacAddress: String) {
        viewModelScope.launch {
            repository.connect(deviceMacAddress)
        }
    }

    fun onDisconnectAction(deviceMacAddress: String) {
        viewModelScope.launch {
            repository.disconnect(deviceMacAddress)
        }
    }

//    private fun executeStartScan() {
//        viewModelScope.launch {
//            try {
//                repository.startScan()
//            } catch (e: LocationPermissionNotGrantedException) {
//                notification.value = Notification.RequestLocationPermission
//            } catch (e: BluetoothPermissionNotGrantedException) {
//                notification.value = Notification.RequestBluetoothPermission
//            }
//        }
//    }
}
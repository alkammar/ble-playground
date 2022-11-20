package com.playground.ble.presentation.devicedetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.ble.data.device.model.Device
import com.playground.ble.data.device.repository.DeviceRepository
import com.playground.ble.presentation.State
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
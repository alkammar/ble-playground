package com.playground.ble.presentation.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playground.ble.data.device.ble.BluetoothPermissionNotGrantedException
import com.playground.ble.data.device.ble.LocationPermissionNotGrantedException
import com.playground.ble.data.device.model.Device
import com.playground.ble.data.device.repository.DeviceRepository
import com.playground.ble.presentation.SingleLiveEvent
import com.playground.ble.presentation.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    val devices: LiveData<State<List<Device>>> get() = _devices
    private val _devices = MutableLiveData<State<List<Device>>>()

    val notification = SingleLiveEvent<DevicesCommand>()

    init {
        _devices.value = State.empty()

        viewModelScope.launch {
            repository.data().collect {
                _devices.value = State.success(it.toList())
            }
        }
    }

    fun onScanAction() {
        executeStartScan()
    }

    fun onLocationPermissionGranted() {
        executeStartScan()
    }

    fun onLocationPermissionDenied() {
        // Explain to the user that the feature is unavailable because the
        // feature requires a permission that the user has denied. At the
        // same time, respect the user's decision. Don't link to system
        // settings in an effort to convince the user to change their
        // decision.
    }

    fun onBluetoothScanPermissionGranted() {
        executeStartScan()
    }

    fun onBluetoothScanPermissionDenied() {
        // Explain to the user that the feature is unavailable because the
        // feature requires a permission that the user has denied. At the
        // same time, respect the user's decision. Don't link to system
        // settings in an effort to convince the user to change their
        // decision.
    }

    private fun executeStartScan() {
        viewModelScope.launch {
            try {
                repository.startScan()
            } catch (e: LocationPermissionNotGrantedException) {
                notification.value = DevicesCommand.RequestLocationPermission
            } catch (e: BluetoothPermissionNotGrantedException) {
                notification.value = DevicesCommand.RequestBluetoothPermission
            }
        }
    }
}
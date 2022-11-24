package ble.playground.central.presentation.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ble.playground.central.data.device.repository.DeviceRepository
import ble.playground.central.data.scanner.repository.ScannerRepository
import ble.playground.central.entity.Device
import ble.playground.central.entity.Scanner
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import ble.playground.common.data.LocationPermissionNotGrantedException
import ble.playground.common.presentation.SingleLiveEvent
import ble.playground.common.presentation.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val scannerRepository: ScannerRepository
) : ViewModel() {

    val scanner: LiveData<State<Scanner>> get() = _scanner
    private val _scanner = MutableLiveData<State<Scanner>>()

    val devices: LiveData<State<List<Device>>> get() = _devices
    private val _devices = MutableLiveData<State<List<Device>>>()

    val notification = SingleLiveEvent<ScannerCommand>()

    init {
        _devices.value = State.empty()

        viewModelScope.launch {
            scannerRepository.data().collect {
                _scanner.value = State.success(it)
            }
        }

        viewModelScope.launch {
            deviceRepository.data().collect {
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
                scannerRepository.startScan()
            } catch (e: LocationPermissionNotGrantedException) {
                notification.value = ScannerCommand.RequestLocationPermission
            } catch (e: BluetoothPermissionNotGrantedException) {
                notification.value = ScannerCommand.RequestBluetoothPermission
            }
        }
    }

    fun onStopScanningAction() {
        viewModelScope.launch {
            scannerRepository.stopScan()
        }
    }
}
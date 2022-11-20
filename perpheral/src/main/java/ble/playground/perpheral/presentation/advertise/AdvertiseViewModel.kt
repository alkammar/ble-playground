package ble.playground.perpheral.presentation.advertise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ble.playground.common.data.BluetoothPermissionNotGrantedException
import ble.playground.common.data.LocationPermissionNotGrantedException
import ble.playground.common.presentation.SingleLiveEvent
import ble.playground.perpheral.data.advertise.AdvertiseRepository
import ble.playground.perpheral.presentation.advertise.AdvertiseCommand.RequestBluetoothPermission
import ble.playground.perpheral.presentation.advertise.AdvertiseCommand.RequestLocationPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvertiseViewModel @Inject constructor(
    private val repository: AdvertiseRepository
) : ViewModel() {

//    val devices: LiveData<State<List<Device>>> get() = _devices
//    private val _devices = MutableLiveData<State<List<Device>>>()

    val notification = SingleLiveEvent<AdvertiseCommand>()

    init {
//        _devices.value = State.empty()
//
//        viewModelScope.launch {
//            repository.data().collect {
//                _devices.value = State.success(it.toList())
//            }
//        }
    }

    fun onAdvertiseAction(progress: Int) {
        executeStartAdvertising(progress)
    }

    fun onLocationPermissionGranted(progress: Int) {
        executeStartAdvertising(progress)
    }

    fun onLocationPermissionDenied() {
        // Explain to the user that the feature is unavailable because the
        // feature requires a permission that the user has denied. At the
        // same time, respect the user's decision. Don't link to system
        // settings in an effort to convince the user to change their
        // decision.
    }

    fun onBluetoothAdvertisePermissionGranted(progress: Int) {
        executeStartAdvertising(progress)
    }

    fun onBluetoothAdvertisePermissionDenied() {
        // Explain to the user that the feature is unavailable because the
        // feature requires a permission that the user has denied. At the
        // same time, respect the user's decision. Don't link to system
        // settings in an effort to convince the user to change their
        // decision.
    }

    private fun executeStartAdvertising(value: Int) {
        viewModelScope.launch {
            try {
                repository.startAdvertising(value.toString())
            } catch (e: LocationPermissionNotGrantedException) {
                notification.value = RequestLocationPermission
            } catch (e: BluetoothPermissionNotGrantedException) {
                notification.value = RequestBluetoothPermission
            }
        }
    }
}
package ble.playground.central.datasource.ble

import ble.playground.central.datasource.ble.model.BleDevice
import ble.playground.central.entity.Sensor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first


suspend fun MutableSharedFlow<Set<BleDevice>>.addAndEmit(bleDevice: BleDevice) {
    with(first().toMutableSet()) {
        add(bleDevice)
        emit(this)
    }
}

suspend fun MutableSharedFlow<Set<BleDevice>>.removeAndEmit(bleDevice: BleDevice) {
    with(first().toMutableSet()) {
        removeIf { it.macAddress == bleDevice.macAddress }
        emit(this)
    }
}

suspend fun MutableSharedFlow<Set<BleDevice>>.updateAndEmit(bleDevice: BleDevice) {
    with(first().toMutableSet()) {
        replace(bleDevice)
        emit(this)
    }
}

private fun MutableSet<BleDevice>.replace(bleDevice: BleDevice) {
    removeIf { it.macAddress == bleDevice.macAddress }
    add(bleDevice)
}

suspend fun MutableSharedFlow<Set<Sensor>>.updateAndEmit(sensor: Sensor) {
    with(first().toMutableSet()) {
        replace(sensor)
        emit(this)
    }
}

private fun MutableSet<Sensor>.replace(sensor: Sensor) {
    removeIf { it.id == sensor.id }
    add(sensor)
}
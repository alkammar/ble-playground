package ble.playground.central.ui.devicedetails

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import ble.playground.central.R
import ble.playground.central.entity.ConnectionState
import ble.playground.central.entity.ConnectionState.*
import ble.playground.central.entity.Device
import ble.playground.central.entity.Sensor
import ble.playground.central.presentation.devicedetails.DeviceDetailsCommand
import ble.playground.central.presentation.devicedetails.DeviceDetailsViewModel
import ble.playground.central.presentation.devicedetails.Operation
import ble.playground.central.ui.BleBackgroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceDetailsFragment : Fragment() {

    private val viewModel: DeviceDetailsViewModel by viewModels()
    private val args by navArgs<DeviceDetailsFragmentArgs>()

    private val macAddress: TextView get() = requireView().findViewById(R.id.device_details_mac_address)
    private val connectionState: TextView get() = requireView().findViewById(R.id.device_details_connection_state)
    private val value: TextView get() = requireView().findViewById(R.id.device_details_value)
    private val connect: Button get() = requireView().findViewById(R.id.device_details_connect_button)

    private var device: Device? = null
    private var operation: Operation? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_device_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        listenToActions()

        viewModel.onViewCreated(args.deviceMacAddress)
    }

    private fun observeViewModel() {

        viewModel.notification.observe(viewLifecycleOwner) { notification ->
            when (notification) {
                is DeviceDetailsCommand.RequestBluetoothPermission -> {
                    this.operation = notification.operation
                    requestBluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            }
        }

        viewModel.device.observe(viewLifecycleOwner) { state ->
            state.data?.let { device ->
                this.device = device
                activity?.title = device.name.ifEmpty { device.id }
                macAddress.text = device.id
                device.connectionState.apply {
                    updateConnectionStateLabel()
                    updateConnectButton()
                    updateBackgroundService()
                }
            }
        }

        viewModel.sensor.observe(viewLifecycleOwner) { state ->
            state.data?.let { sensor ->
                value.text = when(sensor) {
                    is Sensor.Available -> sensor.data
                    is Sensor.NotAvailable -> "--"
                }
            }
        }
    }

    private fun ConnectionState.updateConnectionStateLabel() {
        connectionState.text = when (this) {
            Connected -> "connected"
            Connecting -> "connecting ..."
            Disconnecting -> "disconnecting ..."
            NotConnected -> "not connected"
        }
    }

    private fun ConnectionState.updateBackgroundService() {
        when (this) {
            Connected -> {
                context?.let { context ->
                    startForegroundService(
                        context,
                        Intent(context, BleBackgroundService::class.java)
                    )
                }
            }
            Connecting -> Unit
            Disconnecting -> Unit
            NotConnected -> {
                context?.let { context ->
                    context.stopService(
                        Intent(
                            context,
                            BleBackgroundService::class.java
                        )
                    )
                }
            }
        }
    }

    private fun ConnectionState.updateConnectButton() {
        connect.apply {
            when (this@updateConnectButton) {
                Connected -> {
                    text = getString(R.string.device_details_disconnect_button_label)
                    isEnabled = true
                }
                Connecting -> {
                    text = getString(R.string.device_details_connecting_button_label)
                    isEnabled = false
                }
                Disconnecting -> {
                    text = getString(R.string.device_details_disconnecting_button_label)
                    isEnabled = false
                }
                NotConnected -> {
                    text = getString(R.string.device_details_connect_button_label)
                    isEnabled = true
                }
            }
        }
    }

    private fun listenToActions() {
        connect.setOnClickListener {
            device?.let {
                if (it.connectionState is NotConnected) {
                    viewModel.onConnectAction(args.deviceMacAddress)
                } else if (it.connectionState is Connected) {
                    viewModel.onDisconnectAction(args.deviceMacAddress)
                }
            }
        }
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.all { it.value }) {
            operation?.let { operation ->
                device?.let { device ->
                    viewModel.onBluetoothScanPermissionGranted(operation, device.id)
                    this.operation = null
                }
            }
        } else {
            viewModel.onBluetoothScanPermissionDenied()
        }
    }
}
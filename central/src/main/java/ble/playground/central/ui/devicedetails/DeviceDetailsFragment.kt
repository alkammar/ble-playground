package ble.playground.central.ui.devicedetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import ble.playground.central.R
import ble.playground.central.entity.ConnectionState.*
import ble.playground.central.entity.Device
import ble.playground.central.presentation.devicedetails.DeviceDetailsViewModel
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

        viewModel.device.observe(viewLifecycleOwner) { state ->
            state.data?.let { device ->
                this.device = device
                activity?.title = device.name.ifEmpty { device.id }
                macAddress.text = device.id
                connectionState.text = when (device.connectionState) {
                    Connected -> "connected"
                    Connecting -> "connecting ..."
                    Disconnecting -> "disconnecting ..."
                    NotConnected -> "not connected"
                }
                value.text = "--"
                connect.updateState(device)
            }
        }
    }

    private fun Button.updateState(device: Device) {
        apply {
            when (device.connectionState) {
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
}
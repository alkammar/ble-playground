package com.playground.ble.ui.devices

import android.Manifest.permission.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.playground.ble.R
import com.playground.ble.presentation.devices.DevicesViewModel
import com.playground.ble.presentation.devices.DevicesCommand
import com.playground.ble.presentation.devices.DevicesCommand.RequestBluetoothPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevicesFragment : Fragment() {

    private val list: RecyclerView get() = requireView().findViewById(R.id.devices_list)
    private val scan: View get() = requireView().findViewById(R.id.devices_scan_button)

    private val viewModel: DevicesViewModel by viewModels()
    private val deviceAdapter = DeviceAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_devices, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = deviceAdapter

        observeViewModel()
        listenToActions()
    }

    private fun observeViewModel() {
        viewModel.notification.observe(viewLifecycleOwner) { notification ->
            when (notification) {
                DevicesCommand.RequestLocationPermission -> {
                    requestLocationPermissionLauncher.launch(
                        arrayOf(
                            ACCESS_FINE_LOCATION,
                            ACCESS_COARSE_LOCATION
                        )
                    )
                }
                RequestBluetoothPermission -> {
                    requestBluetoothPermissionLauncher.launch(
                        arrayOf(
                            BLUETOOTH_ADMIN
                        )
                    )
                }
            }
        }

        viewModel.devices.observe(viewLifecycleOwner) { state ->
            state.data?.let { devices ->
                deviceAdapter.items = devices
            }
        }
    }

    private fun listenToActions() {
        deviceAdapter.itemClickListener = {
            findNavController().navigate(DevicesFragmentDirections.actionDevicesSelectDevice(it.id))
        }
        scan.setOnClickListener {
            viewModel.onScanAction()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.all { it.value }) {
            viewModel.onLocationPermissionGranted()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.all { it.value }) {
            viewModel.onBluetoothScanPermissionGranted()
        } else {
            viewModel.onBluetoothScanPermissionDenied()
        }
    }
}
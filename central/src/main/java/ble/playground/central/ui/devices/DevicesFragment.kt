package ble.playground.central.ui.devices

import android.Manifest.permission.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ble.playground.central.R
import ble.playground.central.entity.ScanningState
import ble.playground.central.entity.ScanningState.NotScanning
import ble.playground.central.entity.ScanningState.Scanning
import ble.playground.central.presentation.devices.DevicesViewModel
import ble.playground.central.presentation.devices.DevicesCommand
import ble.playground.central.presentation.devices.DevicesCommand.RequestBluetoothPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DevicesFragment : Fragment() {

    private val list: RecyclerView get() = requireView().findViewById(R.id.devices_list)
    private val scan: Button get() = requireView().findViewById(R.id.devices_scan_button)

    private val viewModel: DevicesViewModel by viewModels()
    private val deviceAdapter = DeviceAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_devices, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = getString(R.string.devices_title)

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
                            BLUETOOTH_SCAN
                        )
                    )
                }
            }
        }

        viewModel.scanner.observe(viewLifecycleOwner) { state ->
            state.data?.let { scanner ->
                scan.text = when(scanner.scanningState) {
                    NotScanning -> getString(R.string.devices_scan_button_label)
                    is Scanning -> getString(R.string.devices_stop_scanning_button_label, 5)
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
            findNavController().navigate(DevicesFragmentDirections.actionDevicesDeviceSelected(it.id))
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
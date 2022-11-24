package ble.playground.central.ui.scanner

import android.Manifest.permission.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ble.playground.central.R
import ble.playground.central.entity.Scanner
import ble.playground.central.entity.ScanningState.NotScanning
import ble.playground.central.entity.ScanningState.Scanning
import ble.playground.central.presentation.devices.DevicesCommand.RequestBluetoothPermission
import ble.playground.central.presentation.devices.DevicesCommand.RequestLocationPermission
import ble.playground.central.presentation.devices.ScannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var timerJob: Job? = null
    private val list: RecyclerView get() = requireView().findViewById(R.id.scanner_device_list)
    private val scan: Button get() = requireView().findViewById(R.id.scanner_scan_button)

    private val viewModel: ScannerViewModel by viewModels()
    private val deviceAdapter = DeviceAdapter()

    private var scanner: Scanner? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_scanner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = getString(R.string.scanner_title)

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = deviceAdapter

        observeViewModel()
        listenToActions()
    }

    private fun observeViewModel() {
        viewModel.notification.observe(viewLifecycleOwner) { notification ->
            when (notification) {
                RequestLocationPermission -> {
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
                this.scanner = scanner
                timerJob?.cancel()
                when (scanner.scanningState) {
                    NotScanning -> {
                        scan.text = getString(R.string.scanner_scan_button_label)
                    }
                    is Scanning -> {
                        timerJob = startTimer(scanner.scanningState.expiresAtMillisecond)
                        scan.text = getString(
                            R.string.scanner_stop_scanning_button_label,
                            (scanner.scanningState.expiresAtMillisecond - Calendar.getInstance().timeInMillis) / 1_000
                        )
                    }
                }
            }
        }

        viewModel.devices.observe(viewLifecycleOwner) { state ->
            state.data?.let { devices ->
                deviceAdapter.items = devices
            }
        }
    }

    private fun startTimer(expiresAtMillisecond: Long) = lifecycleScope.launch {
        withContext(IO) {
            while (expiresAtMillisecond > Calendar.getInstance().timeInMillis) {
                withContext(Main) {
                    whenResumed {
                        scan.text = getString(
                            R.string.scanner_stop_scanning_button_label,
                            (expiresAtMillisecond - Calendar.getInstance().timeInMillis) / 1_000
                        )
                    }
                }
                delay(1_000)
            }
        }
    }

    private fun listenToActions() {
        deviceAdapter.itemClickListener = {
            findNavController().navigate(ScannerFragmentDirections.actionScannerDeviceSelected(it.id))
        }
        scan.setOnClickListener {
            if (scanner?.scanningState is NotScanning) {
                viewModel.onScanAction()
            } else {
                viewModel.onStopScanningAction()
            }
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
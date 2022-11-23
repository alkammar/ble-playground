package ble.playground.perpheral.ui.advertise

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ble.playground.perpheral.R
import ble.playground.perpheral.entity.Advertiser
import ble.playground.perpheral.entity.AdvertisingState.Advertising
import ble.playground.perpheral.entity.AdvertisingState.NotAdvertising
import ble.playground.perpheral.entity.ConnectionState
import ble.playground.perpheral.presentation.advertise.AdvertiseCommand
import ble.playground.perpheral.presentation.advertise.AdvertiseCommand.RequestBluetoothPermission
import ble.playground.perpheral.presentation.advertise.AdvertiseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdvertiseFragment : Fragment() {

    private val seekbar: SeekBar get() = requireView().findViewById(R.id.advertise_seekbar)
    private val value: TextView get() = requireView().findViewById(R.id.advertise_value)
    private val advertise: Button get() = requireView().findViewById(R.id.advertise_button)

    private val viewModel: AdvertiseViewModel by viewModels()

    private var advertiser: Advertiser? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_advertise, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        value.text = seekbar.progress.toString()

        observeViewModel()
        listenToActions()
    }

    private fun observeViewModel() {

        viewModel.advertiser.observe(viewLifecycleOwner) { state ->
            state.data?.let { advertiser ->
                this.advertiser = advertiser
                advertise.text = when (advertiser.advertisingState) {
                    NotAdvertising -> getString(R.string.advertise_not_advertising_button_label)
                    Advertising -> getString(R.string.advertise_advertising_button_label)
                }
            }
        }

        viewModel.notification.observe(viewLifecycleOwner) { notification ->
            when (notification) {
                AdvertiseCommand.RequestLocationPermission -> {
                    requestLocationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                RequestBluetoothPermission -> {
                    requestBluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            }
        }
    }

    private fun listenToActions() {
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                value.text = progress.toString()
                viewModel.onUpdateValue(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        advertise.setOnClickListener {
            advertiser?.let {
                if (it.advertisingState is NotAdvertising) {
                    viewModel.onStartAdvertiseAction()
                } else if (it.advertisingState is Advertising) {
                    viewModel.onStopAdvertiseAction()
                }
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
            viewModel.onBluetoothAdvertisePermissionGranted(seekbar.progress)
        } else {
            viewModel.onBluetoothAdvertisePermissionDenied()
        }
    }
}
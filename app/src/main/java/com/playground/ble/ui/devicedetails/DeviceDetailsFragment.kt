package com.playground.ble.ui.devicedetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.playground.ble.R
import com.playground.ble.presentation.devicedetails.DeviceDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceDetailsFragment : Fragment() {

    private val viewModel: DeviceDetailsViewModel by viewModels()
    private val args by navArgs<DeviceDetailsFragmentArgs>()

    private val macAddress: TextView get() = requireView().findViewById(R.id.device_details_macaddress)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_device_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        println("kammer ??? ${args.deviceMacAddress}")
        viewModel.onViewCreated(args.deviceMacAddress)

        observeViewModel()
    }

    private fun observeViewModel() {

        viewModel.device.observe(viewLifecycleOwner) { state ->
            state.data?.let { device ->
                macAddress.text = device.id
            }
        }
    }
}
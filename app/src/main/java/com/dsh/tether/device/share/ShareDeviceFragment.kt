package com.dsh.tether.device.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.dsh.tether.databinding.FragmentShareDeviceBinding
import com.dsh.tether.device.SelectedDeviceViewModel
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ShareDeviceFragment : Fragment() {

    /**
     * UI layer binder
     */
    private lateinit var binding: FragmentShareDeviceBinding

    /**
     *  [ShareDeviceFragment]'s view model
     */
    private val viewModel: ShareDeviceViewModel by viewModels()

    /**
     * [SelectedDeviceViewModel] holding the device being worked on at the moment
     */
    private val selectedDeviceViewModel: SelectedDeviceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")

        // Initialize UI binder
        binding = FragmentShareDeviceBinding.inflate(layoutInflater)

        // Setup Ui Elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        // Generate setup payload
        val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value!!
        viewModel.shareDevice(thrDevice)

        return binding.root
    }

    private fun setupUiLayerElements() {

    }

    private fun setupObservers() {
        viewModel.sharePayload.observe(viewLifecycleOwner) { payload ->
            Timber.d("Share device payload: [${payload}]")
            if(null == payload){
                return@observe
            }
            binding.tvShareHint.visibility = View.VISIBLE

            // Generate QR code
            val foregroundColor =
                MaterialColors.getColor(
                    requireView(),
                    com.google.android.material.R.attr.colorPrimary
                )
            val backgroundColor =
                MaterialColors.getColor(
                    requireView(),
                    com.google.android.material.R.attr.colorSurface
                )
            viewModel.generateQRCodePayload(
                payload.qrCode,
                foregroundColor = foregroundColor,
                backgroundColor = backgroundColor
            )

            // Format manual entry code
            viewModel.formatManualCodePayload(payload.manualCode)
        }

        // Observe manual pairing code
        viewModel.shareDevicePairingCode.observe(viewLifecycleOwner) {
            Timber.d("The code: $it")
            binding.tvPairingCode.text = it
            binding.llPairingCodeDisplay.visibility = View.VISIBLE
        }

        // Observer QR code
        viewModel.shareDeviceQrCode.observe(viewLifecycleOwner) {qrCode->
            if (qrCode == null) {
                return@observe
            }
            binding.ivQrCode.setImageBitmap(qrCode)
        }

        // Observe shared device status
        viewModel.sharedDeviceStatus.observe(viewLifecycleOwner) {
            Timber.d("Share device status : [${it}]")
        }
    }
}
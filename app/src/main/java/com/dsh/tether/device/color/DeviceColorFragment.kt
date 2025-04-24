package com.dsh.tether.device.color

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dsh.tether.databinding.FragmentDeviceColorBinding
import com.dsh.tether.device.SelectedDeviceViewModel
import com.dsh.tether.device.color.adapter.DeviceColorsAdapter
import com.dsh.matter.model.color.HSVColor
import com.dsh.matter.util.ColorUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DeviceColorFragment : Fragment() {

    /**
     * UI layer binder
     */
    private lateinit var binding : FragmentDeviceColorBinding

    /**
     * [DeviceColorFragment]'s view model
     */
    private val viewModel: DeviceColorViewModel by viewModels()

    /**
     * [SelectedDeviceViewModel] holding precious cargo
     */
    private val selectedDeviceViewModel: SelectedDeviceViewModel by activityViewModels()

    /**
     * The current color states of the selected device
     */
    private var colorId = 0
    private var color : HSVColor = HSVColor(0,0, 100)

    /**
     * Device colors list recycler view adapter
     */
    private val deviceColorsAdapter =
        DeviceColorsAdapter { colorId ->
            this@DeviceColorFragment.colorId = colorId
            val color = try {
                // convert Android color to headless RGB hex color space
                val colorHex = String.format("#%06X", (0xFFFFFF.and( colorId)))
                ColorUtils.toHSV(colorHex) ?: return@DeviceColorsAdapter
            }catch (ex: Exception) {
                return@DeviceColorsAdapter
            }

            this.color = color
            val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value!!
            viewModel.updateColor(thrDevice.id, color)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")

        // Initialize UI later binder
        binding = FragmentDeviceColorBinding.inflate(layoutInflater)

        // Setup Ui Elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        // Populate devices color list
        val supportedDeviceColors = ColorUtils.getBaseDeviceColors(requireContext())
        deviceColorsAdapter.submitList(supportedDeviceColors)

        return binding.root
    }

    private fun setupUiLayerElements() {
        // navigate back
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // setup device colors recycler view adapter
        binding.rvDeviceColors.adapter = deviceColorsAdapter
    }

    private fun setupObservers() {

    }
}
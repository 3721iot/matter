package com.dsh.tether.device.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import com.dsh.data.model.device.DeviceState
import com.dsh.data.repository.DeviceStatesRepo
import com.dsh.matter.model.device.DeviceType
import com.dsh.matter.model.device.FanMode
import com.dsh.matter.util.device.FanModeUtil
import com.dsh.tether.R
import com.dsh.tether.databinding.FragmentDeviceControllerBinding
import com.dsh.tether.device.SelectedDeviceViewModel
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DeviceControllerFragment : Fragment() {

    /**
     * Repo dependencies injections
     */
    @Inject
    internal lateinit var deviceStatesRepo: DeviceStatesRepo

    /**
     *
     */
    private lateinit var binding : FragmentDeviceControllerBinding

    /**
     * [DeviceControllerFragment]'s view model
     */
    private val viewModel: DeviceControllerViewModel by viewModels()

    /**
     * [SelectedDeviceViewModel] holding the current device's data
     */
    private val selectedDeviceViewModel: SelectedDeviceViewModel by activityViewModels()

    /**
     * The current [on] and [online] states of the selected device
     */
    private var on = false
    private var online = false
    private var brightness = 0
    private var colorTemperature = 0
    private var fanSpeed = 0
    private var fanMode = FanMode.Off

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onCreateView")

        // Setup device fragment's binding
        binding = FragmentDeviceControllerBinding.inflate(inflater,container,false)

        // Setup Ui Elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                Timber.d("[*** LifeCycle ***] : onResume")
                val selectedDeviceIdLiveData = selectedDeviceViewModel.selectedDeviceIdLiveData
                viewModel.startStatesMonitoring(selectedDeviceIdLiveData.value!!)
            }

            override fun onPause(owner: LifecycleOwner) {
                Timber.d("[*** LifeCycle ***] : onPause")
                val selectedDeviceIdLiveData = selectedDeviceViewModel.selectedDeviceIdLiveData
                viewModel.stopStatesMonitoring(selectedDeviceIdLiveData.value!!)
            }
        })
    }

    private fun setupUiLayerElements() {
        // Navigate to device settings
        binding.ivDeviceSettings.setOnClickListener {
            view?.findNavController()
                ?.navigate(R.id.action_device_controller_to_device_settings_fragment)
        }

        // Toggle power
        binding.cvPowerSwitch.setOnClickListener {
            val powerStatus = !on
            val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value!!
            viewModel.updateSwitchStatus(thrDevice.id, thrDevice.type, powerStatus)
        }

        // Open colors list
        binding.llColor.setOnClickListener {
            view?.findNavController()
                ?.navigate(R.id.action_device_controller_to_device_color_fragment)
        }

        // Slider touch lister
        binding.sldBrightness.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    // Responds to when slider's touch event is being started
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    // Responds to when slider's touch event is being stopped
                    val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value!!
                    viewModel.updateBrightness(thrDevice.id, brightness)
                }
            }
        )

        binding.sldBrightness.addOnChangeListener { _, value, fromUser ->
            // Response when slider's value is changed
            if(fromUser) {
                brightness = value.toInt()
                binding.tvBrightness.text = brightness.toString()
            }
        }

        // Button toggle group listener
        binding.btnGrpFanModes.addOnButtonCheckedListener { _, checkedId, _ ->
            val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value!!
            when(checkedId) {
                binding.btnFanModeLow.id -> {
                    viewModel.updateFanMode(thrDevice.id, FanMode.Low)
                }
                binding.btnFanModeMedium.id ->{
                    viewModel.updateFanMode(thrDevice.id, FanMode.Medium)
                }
                binding.btnFanModeHigh.id ->{
                    viewModel.updateFanMode(thrDevice.id, FanMode.High)
                }
            }
        }
    }

    private fun setupObservers() {
        // Observe the state of the device in this screen
        deviceStatesRepo.lastUpdatedDeviceState.observe(viewLifecycleOwner) { deviceState->
            updateDeviceInfo(deviceState)
        }
    }

    private fun updateDeviceInfo(deviceState: DeviceState) {
        val selectedDeviceIdLiveData = selectedDeviceViewModel.selectedDeviceIdLiveData
        if(selectedDeviceIdLiveData.value == -1L){
            online = false
            Timber.e("Something is not right")
            return
        }

        // Current selected device
        val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value
        thrDevice?.let {
            online = deviceState.online
            on = deviceState.on
            brightness = deviceState.brightness
            colorTemperature = deviceState.colorTemperature
            fanMode = FanModeUtil.toEnum(deviceState.fanMode)
            fanSpeed = deviceState.fanSpeed

            // update device name
            binding.tvDeviceName.text = thrDevice.name

            // Update ui elements
            updateDeviceStatus(online, on)
            updatePowerUiElements(online, on)
            when(thrDevice.type.toLong()) {
                DeviceType.DimmableLight.type,
                DeviceType.ExtendedColorLight.type,
                DeviceType.ColorTemperatureLight.type-> {
                    updateBrightnessUiElements(brightness, on && online)
                }
                DeviceType.Fan.type-> {
                    binding.llColor.visibility = View.INVISIBLE
                    binding.rlBrightness.visibility= View.GONE
                    updateFanModeElements(fanMode, on && online)
                }
                else -> {
                    binding.llColor.visibility = View.INVISIBLE
                    binding.rlBrightness.visibility= View.GONE
                }
            }
        }
    }

    /**
     * Updates device power status ui
     * @param online device's online status
     * @param power device's switch/power status
     */
    private fun updatePowerUiElements(online: Boolean, power: Boolean) {
        val onIcon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_power_on_round)
        val offIcon =
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_power_off_round)
        val powerIcon = if (power && online) onIcon else offIcon
        binding.ivPowerIc.setImageDrawable(powerIcon)
        binding.ivPowerIc.isEnabled = online
        binding.cvPowerSwitch.tag = power
    }

    /**
     * Update device status ui elements
     *
     * @param online device's online status
     * @param power device's power/switch status
     */
    private fun updateDeviceStatus(online: Boolean, power: Boolean) {
        // update device online/power  status
        if (online) {
            binding.tvDeviceStatus.text =
                if (power) getString(R.string.power_on) else getString(R.string.power_off)
        } else {
            binding.tvDeviceStatus.text = getString(R.string.offline)
        }
        binding.tvDeviceStatus.tag = online
    }

    /**
     * Update device's brightness ui elements
     *
     * @param brightness device's brightness
     * @param isVisible whether the brightness slider should be visible or not
     */
    private fun updateBrightnessUiElements(brightness: Int, isVisible: Boolean) {
        if (brightness <= 0) {
            return
        }
        binding.sldBrightness.value = brightness.toFloat()
        binding.tvBrightness.tag = brightness
        binding.tvBrightness.text = brightness.toString()
        binding.rlBrightness.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * Update device's fan ui elements
     *
     * @param fanMode fan mode
     * @param isVisible whether the modes should be visible or not
     */
    private fun updateFanModeElements(fanMode: FanMode, isVisible: Boolean) {
        Timber.d("Fan mode: [${fanMode}]")
        // ToDo() highlight the selected mode on subscription report
        binding.btnGrpFanModes.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
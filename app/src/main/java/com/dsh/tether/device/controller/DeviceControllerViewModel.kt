package com.dsh.tether.device.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsh.data.repository.DeviceStatesRepo
import com.dsh.matter.management.device.DeviceController
import com.dsh.matter.management.device.DeviceControllerCallback
import com.dsh.matter.management.device.DeviceStatesManager
import com.dsh.matter.management.device.DeviceSubscriptionListener
import com.dsh.matter.model.color.HSVColor
import com.dsh.matter.model.device.DeviceType
import com.dsh.matter.model.device.FanMode
import com.dsh.matter.model.device.StateAttribute
import com.dsh.matter.util.device.UnsupportedDeviceTypeException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class DeviceControllerViewModel @Inject constructor(
    private val deviceController: DeviceController,
    private val deviceStatesRepo: DeviceStatesRepo,
    private val deviceStatesManager: DeviceStatesManager,
) : ViewModel() {

    /**
     * Update device's switch/power status
     *
     * @param deviceId device identifier
     * @param deviceType device type
     * @param on power status
     */
    fun updateSwitchStatus(deviceId: Long, deviceType: String,  on: Boolean) {
        Timber.d("Updating device status: deviceId=[${deviceId}], on=[${on}]")
        when(deviceType.toLong()) {
            DeviceType.Fan.type->{
                updateFanMode(deviceId, if(on) FanMode.High else FanMode.Off)
            }
            else -> {
                updatePower(deviceId, on)
            }
        }
    }

    /**
     * Update device's switch/power status
     *
     * @param deviceId device identifier
     * @param on power status
     */
    private fun updatePower(deviceId: Long, on: Boolean) {
        val deviceControllerCallback = object : DeviceControllerCallback() {
            override fun onSuccess() {
                viewModelScope.launch {
                    deviceStatesRepo.updateDeviceState(
                        deviceId = deviceId, online = true, on = on
                    )
                }
            }

            override fun onError(error: Exception?) {
                Timber.d("Failed to execute command. Cause: ${error?.localizedMessage}")
                viewModelScope.launch {
                    deviceStatesRepo.updateDeviceState(
                        deviceId = deviceId, online = false
                    )
                }
            }
        }

        // publish device power command
        viewModelScope.launch {
            deviceController.power(
                deviceId = deviceId, on = on, callback = deviceControllerCallback
            )
        }
    }

    /**
     * Update fan mode
     *
     * @param deviceId device identifier
     * @param fanMode fan mode
     */
    fun updateFanMode(deviceId: Long, fanMode: FanMode) {
        val deviceControllerCallback = object : DeviceControllerCallback() {
            override fun onSuccess() {
                viewModelScope.launch {
                    deviceStatesRepo.updateDeviceState(
                        deviceId = deviceId,
                        online = true,
                        on = (fanMode != FanMode.Off),
                        fanMode = fanMode.mode
                    )
                }
            }

            override fun onError(error: Exception?) {
                Timber.d("Failed to execute command. Cause: ${error?.localizedMessage}")
                viewModelScope.launch {
                    deviceStatesRepo.updateDeviceState(
                        deviceId = deviceId, online = false
                    )
                }
            }
        }

        // publish fan mode command
        viewModelScope.launch {
            deviceController.fanMode(
                deviceId = deviceId, fanMode , callback = deviceControllerCallback
            )
        }
    }

    /**
     * Update device's brightness
     *
     * @param deviceId device identifier
     * @param brightness brightness value
     */
    fun updateBrightness(deviceId: Long, brightness: Int) {
        // ToDo() might want to switch off the device if brightness is 0
        val deviceControllerCallback = object : DeviceControllerCallback() {
            override fun onSuccess() {
                viewModelScope.launch {
                    deviceStatesRepo.updateDeviceState(
                        deviceId = deviceId, online = true, brightness = brightness
                    )
                }
            }

            override fun onError(error: Exception?) {
                Timber.d("Failed to execute command. Cause: ${error?.localizedMessage}")
                viewModelScope.launch {
                    deviceStatesRepo.updateDeviceState(
                        deviceId = deviceId, online = false
                    )
                }
            }
        }

        // publish device brightness command
        viewModelScope.launch {
            deviceController.brightness(
                deviceId = deviceId, brightness = brightness, callback = deviceControllerCallback
            )
        }
    }

    /**
     * Update device's brightness
     *
     * @param deviceId device identifier
     * @param temperature color temperature value
     */
    fun updateColorTemperature(deviceId: Long, temperature: Int) {
        val deviceControllerCallback = object : DeviceControllerCallback() {
            override fun onSuccess() {
                Timber.d("Color temperature set successfully")
            }

            override fun onError(error: Exception?) {
                Timber.d("Failed to execute command. Cause: ${error?.localizedMessage}")
            }
        }

        // publish device color temperature command
        viewModelScope.launch {
            deviceController.colorTemperature(
                deviceId = deviceId, temperature = temperature, callback = deviceControllerCallback
            )
        }
    }

    /**
     * Start monitoring device states
     */
    fun startStatesMonitoring(deviceId: Long) {
        viewModelScope.launch {
            val listener = object : DeviceSubscriptionListener() {
                override fun onError(ex: Exception) {
                    Timber.e("Failed to monitor states. Cause: ${ex.localizedMessage}")
                }

                override fun onSubscriptionEstablished(subscriptionId: ULong) {
                    Timber.d("Subscription established: $subscriptionId")
                }

                override fun onReport(report: HashMap<StateAttribute, Any>) {
                    updateStates(deviceId, report)
                }
            }
            deviceStatesManager.subscribe(deviceId, listener)
        }
    }

    /**
     * Update device states
     *
     * @param deviceId device identifier
     */
    private fun updateStates(deviceId: Long, deviceStates :HashMap<StateAttribute, Any>){
        viewModelScope.launch {
            try {
                // ToDo() do something about the pattern below. Maybe use
                //  device type for crying out loud
                deviceStatesRepo.updateDeviceState(
                    deviceId = deviceId,
                    online = if(deviceStates[StateAttribute.Online] != null){
                        deviceStates[StateAttribute.Online] as Boolean
                    }else{false},
                    on = if (deviceStates[StateAttribute.Switch] != null){
                        deviceStates[StateAttribute.Switch] as Boolean
                    }else { null },
                    hue = if (deviceStates[StateAttribute.Color] != null) {
                        (deviceStates[StateAttribute.Color] as HSVColor).hue
                    }else{ null},
                    saturation = if(deviceStates[StateAttribute.Color] != null){
                        (deviceStates[StateAttribute.Color] as HSVColor).saturation
                    }else{null},
                    brightness = if(deviceStates[StateAttribute.Brightness] != null){
                        deviceStates[StateAttribute.Brightness] as Int
                    }else{null},
                    colorTemperature = if(deviceStates[StateAttribute.ColorTemperature] != null){
                        deviceStates[StateAttribute.ColorTemperature] as Int
                    }else{null},
                    fanMode = if(deviceStates[StateAttribute.FanMode] != null) {
                        (deviceStates[StateAttribute.FanMode] as FanMode).mode
                    }else{null},
                    fanSpeed = if(deviceStates[StateAttribute.FanSpeed] != null) {
                        deviceStates[StateAttribute.FanSpeed] as Int
                    }else{null}
                )
            }catch (ex: UnsupportedDeviceTypeException){
                Timber.e(ex.localizedMessage)
            } catch (ex: Exception) {
                Timber.e(ex,"Something is not right. Cause: ${ex.localizedMessage}")
            }
        }
    }

    /**
     * Stop monitoring device states
     */
    fun stopStatesMonitoring(deviceId: Long) {
        viewModelScope.launch {
            try{
                deviceStatesManager.unsubscribe(deviceId)
            }catch (ex: Exception) {
                Timber.e("Failed to shutdown subscriptions")
            }
        }
    }
}
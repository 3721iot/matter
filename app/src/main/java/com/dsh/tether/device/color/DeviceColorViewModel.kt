package com.dsh.tether.device.color

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsh.data.repository.DeviceStatesRepo
import com.dsh.matter.management.device.DeviceController
import com.dsh.matter.management.device.DeviceControllerCallback
import com.dsh.matter.model.color.HSVColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceColorViewModel @Inject constructor(
    private val deviceController: DeviceController,
    private val deviceStatesRepo: DeviceStatesRepo,
) : ViewModel() {

    /**
     *  List of default device colors
     */
    private val _deviceColorsLiveData = MutableLiveData<List<Int>>()
    val deviceColorsLiveData : LiveData<List<Int>>
        get() = _deviceColorsLiveData

    /**
     * Update device's switch/power status
     *
     * @param deviceId device identifier
     * @param color power status
     */
    fun updateColor(deviceId: Long, color: HSVColor) {
        Timber.d("Updating device color: deviceId=[${deviceId}], color=[${color}]")
        val deviceControllerCallback = object : DeviceControllerCallback() {
            override fun onSuccess() {
                viewModelScope.launch {
                    deviceStatesRepo.updateDeviceState(
                        deviceId = deviceId,
                        online = true,
                        hue = color.hue,
                        saturation = color.saturation
                    )
                }
            }

            override fun onError(error: Exception?) {
                Timber.d("Failed to execute command. Cause: ${error?.localizedMessage}")
                if(error is IllegalStateException){
                    viewModelScope.launch {
                        deviceStatesRepo.updateDeviceState(
                            deviceId = deviceId, online = false
                        )
                    }
                }
            }
        }

        // publish device power command
        viewModelScope.launch {
            deviceController.color(
                deviceId = deviceId, color = color, callback = deviceControllerCallback
            )
        }
    }
}
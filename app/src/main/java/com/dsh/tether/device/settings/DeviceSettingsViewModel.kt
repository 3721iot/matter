package com.dsh.tether.device.settings

import androidx.lifecycle.*
import com.dsh.data.repository.DeviceStatesRepo
import com.dsh.data.repository.DevicesRepo
import com.dsh.matter.management.device.DeviceDecommissionCallback
import com.dsh.matter.management.device.DeviceManager
import com.dsh.tether.model.GeneralTaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceSettingsViewModel @Inject constructor(
    private val devicesRepo: DevicesRepo,
    private val deviceManager: DeviceManager,
    private val deviceStatesRepo: DeviceStatesRepo
) : ViewModel() {

    /**
     *
     */
    private val _deleteDeviceTask = MutableLiveData<GeneralTaskStatus>(GeneralTaskStatus.NotStarted)
    val deleteDeviceTaskLiveData : LiveData<GeneralTaskStatus>
        get() = _deleteDeviceTask

    /**
     *
     */
    private val _deviceUniqueId = MutableLiveData<String>()
    val deviceUniqueIdLiveData : LiveData<String>
        get() = _deviceUniqueId

    /**
     * Deletes device from home
     *
     * @param deviceId device identifier
     */
    fun deleteDevice(deviceId: Long) {
        Timber.d("Deleting device :$deviceId")
        _deleteDeviceTask.postValue(GeneralTaskStatus.InProgress)
        val callback = object : DeviceDecommissionCallback() {
            override fun onError(code: Int, message: String) {
                Timber.d("Delete failed")
                _deleteDeviceTask.postValue(GeneralTaskStatus.Failed(message))
            }

            override fun onSuccess(deviceId: Long) {
                Timber.d("Device successfully deleted")
                _deleteDeviceTask.postValue(GeneralTaskStatus.Completed())
            }
        }

        try {
            viewModelScope.launch {
                devicesRepo.removeDevice(deviceId)
                deviceStatesRepo.removeDevice(deviceId)
                deviceManager.decommissionDevice(deviceId, callback)
            }
        }catch (ex: Exception) {
            Timber.e("Failed to delete device info. Cause: ${ex.localizedMessage}")
            _deleteDeviceTask.postValue(ex.localizedMessage?.let {
                GeneralTaskStatus.Failed(it, ex)
            })
        }
    }

    /**
     * Updates device name
     *
     * @param deviceId device identifier
     * @param name device name
     */
    fun updateDeviceName(deviceId : Long, name: String) {
        Timber.d("Device: nodeId=$deviceId, deviceName=$name")
        viewModelScope.launch {
            try {
                val status = deviceManager.renameDevice(deviceId, name)
                Timber.d("Task status: $status")

                // update repository
                devicesRepo.updateDeviceName(deviceId, name)
            } catch (ex: Exception) {
                val errorMsg = "Failed to update device name: [${deviceId}]=[${name}]"
                Timber.e(ex, errorMsg)
            }
        }
    }

    /**
     * Unique ID getter
     *
     * @param deviceId device identifier
     */
    fun getDeviceUniqueId(deviceId: Long) {
        Timber.d("Device: nodeId=$deviceId")
        viewModelScope.launch {
            try {
                val uniqueId =  deviceManager.getDeviceUniqueId(deviceId)
                Timber.d("Device unique Id: $uniqueId")
            }catch (ex: Exception) {
                val errorMsg = "Failed to get unique id for device: $deviceId"
                Timber.d(ex, errorMsg)
            }
        }
    }
}
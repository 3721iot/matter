package com.dsh.tether.commission.provisioning

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsh.data.model.device.Device
import com.dsh.data.model.device.Metadata
import com.dsh.data.repository.DeviceStatesRepo
import com.dsh.data.repository.DevicesRepo
import com.dsh.matter.model.wifi.WiFiCredentials
import com.dsh.matter.management.device.DeviceCommissionCallback
import com.dsh.matter.management.device.DeviceManager
import com.dsh.matter.model.CommissioningErrorCode
import com.dsh.matter.model.device.MtrDevice
import com.dsh.tether.model.CommissioningTaskStatus
import com.dsh.tether.model.SaveTaskStatus
import com.dsh.tether.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceProvisioningViewModel @Inject constructor(
    private val devicesRepo: DevicesRepo,
    private val deviceManager: DeviceManager,
    private val deviceStatesRepo: DeviceStatesRepo,
) : ViewModel() {

    /**
     * The current status of the commission device action sent via [commissionDevice]
     */
    private val _commissionDeviceStatus = MutableLiveData<CommissioningTaskStatus>(CommissioningTaskStatus.NotStarted)
    val commissionDeviceStatus : LiveData<CommissioningTaskStatus>
        get() = _commissionDeviceStatus

    /**
     * The current status of the device being saved in the repo or uploaded somewhere
     */
    private val _saveDeviceTaskStatus = MutableLiveData<SaveTaskStatus>(SaveTaskStatus.NotStarted)
    val saveDeviceTaskStatus : LiveData<SaveTaskStatus>
        get() = _saveDeviceTaskStatus

    /**
     * WiFi credentials request live data
     */
    private val _requestWifiCredentials = MutableLiveData<Long>()
    val requestWifiCredentials: LiveData<Long>
        get() = _requestWifiCredentials


    fun commissionDevice(payload: String) {
        // Update commission device status
        _commissionDeviceStatus.postValue(CommissioningTaskStatus.InProgress)

        viewModelScope.launch {
            deviceManager.startCommissioningDevice(
                payload,
                object : DeviceCommissionCallback() {
                    override fun onError(code: CommissioningErrorCode, message: String?) {
                        Timber.e("Failed to commission device: [${code}], $message")
                        _commissionDeviceStatus.postValue(
                            CommissioningTaskStatus.Failed(code, message!!)
                        )
                    }

                    override fun onAttestationFailure(
                        code: CommissioningErrorCode,
                        devicePtr: Long
                    ) {
                        val msg = "Device attestation failed"
                        Timber.w(msg)
                        _commissionDeviceStatus.postValue(
                            CommissioningTaskStatus.Warning(code, msg, devicePtr)
                        )
                    }

                    override fun onSuccess(mtrDevice: MtrDevice) {
                        Timber.d("Commissioning completed successfully: [${mtrDevice}]")
                        saveDevice(mtrDevice)
                    }

                    override fun onWiFiCredentialsRequired(devicePtr: Long) {
                        Timber.d("Requesting WiFi credentials for device: $devicePtr")
                        _requestWifiCredentials.postValue(devicePtr)
                    }
                }
            )
        }
    }

    /**
     * Continues device commissioning on attestation failure
     *
     * @param devicePtr device pointer
     * @param ignoreFailure whether to ignore attestation failure or not
     */
    fun continueCommissioning(devicePtr: Long, ignoreFailure: Boolean) {
        viewModelScope.launch {
            deviceManager.continueCommissioningDevice(devicePtr, ignoreFailure)
        }
    }

    /**
     * Continues device commissioning with WiFi credentials
     *
     * @param devicePtr device pointer
     * @param ssid wifi ssid
     * @param password wifi password
     */
    fun continueCommissioning(devicePtr: Long, ssid: String, password: String) {
        viewModelScope.launch {
            val credentials = WiFiCredentials(ssid = ssid, password = password)
            deviceManager.continueCommissioningDevice(devicePtr, credentials)
        }
    }

    /**
     * Called to save the device when device commissioning has completed successfully
     *
     * @param mtrDevice commissioned matter device
     */
    private fun saveDevice(mtrDevice: MtrDevice){
        Timber.d("Commissioned Device: info=[${mtrDevice}]")
        // Adding commissioned device to repo
        viewModelScope.launch {
            try {
                // Build device metadata
                val metadata = Metadata.newBuilder()
                    .setCommissioningFlow(
                        mtrDevice.setupPayloadDescriptor.commissioningFlow
                    )
                    .setDiscriminator(mtrDevice.setupPayloadDescriptor.discriminator)
                    .setVersion(mtrDevice.setupPayloadDescriptor.version)
                    .setHasShortDiscriminator(
                        mtrDevice.setupPayloadDescriptor.hasShortDiscriminator
                    )
                    .setSetupPinCode(mtrDevice.setupPayloadDescriptor.setupPinCode)
                    .addAllDiscoveryCapabilities(
                        mtrDevice.setupPayloadDescriptor.discoveryCapabilities.map {
                            it
                        }
                    )

                // Build device object
                val device = Device.newBuilder()
                    .setName(mtrDevice.name)
                    .setDeviceId(mtrDevice.id)
                    .setDeviceType(mtrDevice.deviceDescriptor.deviceType)
                    .setDateCommissioned(TimeUtils.getTimestamp())
                    .setProductId(mtrDevice.deviceDescriptor.productId.toString())
                    .setVendorId(mtrDevice.deviceDescriptor.vendorId.toString())
                    .setMetadata(metadata)
                    .build()

                // Add device to repo
                devicesRepo.addDevice(device)

                // Post operation's live data
                _commissionDeviceStatus.postValue(CommissioningTaskStatus.Completed())
            }catch (ex: Exception){
                val errMsg = "Failed to save device"
                Timber.e(ex, errMsg)
                // Post operation error's live data
                _commissionDeviceStatus.postValue(
                    CommissioningTaskStatus.Failed(CommissioningErrorCode.AddDeviceFailed, errMsg)
                )
                return@launch
            }

            try {
                // Set default states
                deviceStatesRepo.addDeviceState(deviceId=mtrDevice.id, online=true)
            }catch (ex: Exception) {
                Timber.e("Failed to set device's default state")
            }
        }
    }
}
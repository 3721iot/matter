package com.dsh.tether.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dsh.tether.model.MtrDeviceMetadata
import com.dsh.tether.model.TetherDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SelectedDeviceViewModel @Inject constructor() : ViewModel() {

    /**
     * The identifier of the currently selected device
     * @note Might need to use Flows and StateFlows for consistency
     */
    private val _selectedDeviceIdLiveData = MutableLiveData(-1L)
    val selectedDeviceIdLiveData : LiveData<Long>
    get() = _selectedDeviceIdLiveData

    /**
     * The data for the selected device
     */
    private val _selectedDeviceLiveData = MutableLiveData(getDefaultTetherDevice())
    val selectedDeviceLiveData : LiveData<TetherDevice>
    get() = _selectedDeviceLiveData

    /**
     * Sets the selected device and it's device identifier
     * @param thrDevice device data
     */
    fun setDevice(thrDevice: TetherDevice) {
        _selectedDeviceLiveData.value = thrDevice
        _selectedDeviceIdLiveData.value = thrDevice.id
    }

    /**
     * Resets all the selected device [LiveData]
     */
    fun reset() {
        _selectedDeviceLiveData.value = null
        _selectedDeviceIdLiveData.value = -1L
    }

    /**
     * Generate default tether device
     */
    private fun getDefaultTetherDevice(): TetherDevice{
        val metadata = MtrDeviceMetadata(
            vendorId = 0,
            productId = 0,
            version = 0,
            discriminator = 0,
            setupPinCode = 0L,
            commissioningFlow = 0,
            hasShortDiscriminator = false,
            discoveryCapabilities = HashSet()
        )

        return TetherDevice(
            id = 0,
            name = "Matter Device",
            home = "Someone",
            room = "Attic",
            type = "0",
            states = HashMap(),
            metadata = metadata
        )
    }

    /**
     * Updates the selected device name
     *
     * @param deviceName device name
     */
    fun updateDeviceName(deviceName: String) {
        _selectedDeviceLiveData.value?.name = deviceName
    }
}
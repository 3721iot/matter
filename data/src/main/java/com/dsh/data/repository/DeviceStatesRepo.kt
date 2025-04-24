package com.dsh.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dsh.data.model.device.DeviceState
import com.dsh.data.model.device.DeviceStates
import com.dsh.data.model.device.HSVColor
import com.dsh.data.serializer.deviceSatesDataStore
import com.google.protobuf.Timestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStatesRepo @Inject constructor(@ApplicationContext context: Context) {

    /**
     * Devices state data store
     */
    private val deviceStatesDataStore = context.deviceSatesDataStore

    /**
     * Devices state flow
     */
    val deviceStatesFlow : Flow<DeviceStates> =
        deviceStatesDataStore.data.catch { ex->
            if(ex is IOException) {
                val errorMsg = "Failed to read device state."
                Timber.e(errorMsg)
                emit(DeviceStates.getDefaultInstance())
            }
        }

    /**
     * Latest device state update
     */
    private val _lastUpdatedDeviceState =  MutableLiveData(DeviceState.getDefaultInstance())
    val lastUpdatedDeviceState: LiveData<DeviceState>
        get() =  _lastUpdatedDeviceState

    /**
     * Adds a new device state to the repo
     * @param deviceId device identifier
     * @param online whether the device is online or not
     * @param on whether device is on or not
     * @param hue device's hue
     * @param saturation device's saturation
     * @param brightness device's brightness
     */
    suspend fun addDeviceState(
        deviceId: Long,
        online: Boolean,
        on: Boolean = false,
        hue: Int = 0,
        saturation: Int = 0,
        brightness: Int = 0
    ) {
        val deviceState = DeviceState.newBuilder()
            .setDeviceId(deviceId)
            .setDateCaptured(getTimestamp())
            .setOnline(online)
            .setOn(on)
            .setBrightness(brightness)
            .setColor(
                HSVColor.newBuilder()
                    .setHue(hue)
                    .setSaturation(saturation)
                    .setValue(100)
            )
            .build()

        deviceStatesDataStore.updateData { deviceStates->
            deviceStates.toBuilder().addDeviceStates(deviceState).build()
        }

        _lastUpdatedDeviceState.value = deviceState
    }

    /**
     * Updates device state
     * @param deviceId device identifier
     * @param online whether the device is online or not
     * @param on whether the device is on or not
     * @param hue device's hue
     * @param saturation device's saturation
     * @param brightness device's brightness
     * @param colorTemperature color temperature
     * @param fanSpeed fan speed
     * @param fanMode fan mode
     */
    suspend fun updateDeviceState(
        deviceId: Long,
        online: Boolean,
        on: Boolean? = null,
        hue: Int?=null,
        saturation: Int?=null,
        brightness: Int?=null,
        colorTemperature: Int?=null,
        fanSpeed: Int?=null,
        fanMode: Int?=null
    ) {
        val deviceStateBuilder = DeviceState.newBuilder()
            .setDeviceId(deviceId)
            .setOnline(online)
            .setDateCaptured(getTimestamp())

        val deviceStates = deviceStatesFlow.first()
        val devicesStateCnt = deviceStates.deviceStatesCount
        var updateCompleted = false

        for (index in 0 until devicesStateCnt) {
            val state =  deviceStates.getDeviceStates(index)
            if(deviceId != state.deviceId) {
                continue
            }

            val deviceState = deviceStateBuilder
                .setOn(on ?: state.on)
                .setBrightness(brightness ?: state.brightness)
                .setColor(
                    HSVColor.newBuilder()
                        .setHue(hue ?: state.color.hue)
                        .setSaturation(saturation ?: state.color.saturation)
                        .setValue(100)
                )
                .setColorTemperature(colorTemperature ?: state.colorTemperature)
                .setFanMode(fanMode ?: state.fanMode)
                .setFanSpeed(fanSpeed ?: state.fanSpeed)
                .build()

            deviceStatesDataStore.updateData { deviceStatesList ->
                deviceStatesList.toBuilder().setDeviceStates(index, deviceState).build()
            }
            _lastUpdatedDeviceState.value = deviceState
            updateCompleted = true
            break
        }
        if(!updateCompleted) {
            val logMsg = "For some strange reason, the device [${deviceId}] was not found in repo."
            Timber.w(logMsg)
            addDeviceState(deviceId=deviceId, online=online)
        }
    }

    /**
     * Returns all the devices state
     * @return the list of all the device states
     */
    suspend fun getAllDeviceStates() : DeviceStates {
        return deviceStatesFlow.first()
    }

    /**
     * Finds the index for a given device identifier
     * @param deviceId device identifier
     *@return the index of the device state
     */
    private suspend fun getIndex(deviceId: Long): Int {
        val deviceStates = deviceStatesFlow.first()
        val devicesCnt  =  deviceStates.deviceStatesCount
        for (index in 0 until devicesCnt){
            val device = deviceStates.getDeviceStates(index)
            if(deviceId == device.deviceId){
                return index
            }
        }
        return -1
    }

    /**
     * Removes device's states from repo
     * @param deviceId device identifier
     */
    suspend fun removeDevice(deviceId: Long) {
        Timber.d("Deleting device with id : [${deviceId}]")
        val index = getIndex(deviceId)
        if(index == -1){
            throw Exception("Device not found: $deviceId")
        }

        // remove device states from data store
        deviceStatesDataStore.updateData { deviceStates ->
            deviceStates.toBuilder().removeDeviceStates(index).build()
        }
    }

    /**
     * Returns a com.google.protobuf.Timestamp for the current time.
     *
     * @return the timestamp
     */
    private fun getTimestamp(): Timestamp {
        val now = Instant.now()
        return Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
    }
}
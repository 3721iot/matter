package com.dsh.data.repository

import android.content.Context
import com.dsh.data.model.device.Device
import com.dsh.data.model.device.Devices
import com.dsh.data.serializer.deviceDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevicesRepo @Inject constructor(@ApplicationContext context: Context) {

    /**
     * Devices data store
     */
    private val devicesDataStore = context.deviceDataStore

    val devicesFlow: Flow<Devices> =
        devicesDataStore.data.catch { ex->
            if(ex is IOException){
                val errorMsg = "Failed to read devices"
                Timber.e(ex, errorMsg)
                emit(Devices.getDefaultInstance())
            }else{
                throw ex
            }
        }

    /**
     * Add a device to the list of device
     * @param device device
     */
    suspend fun addDevice(device: Device) {
        Timber.d("Adding device: [${device}]")
        devicesDataStore.updateData { devices ->
            devices.toBuilder().addDevices(device).build()
        }
    }

    /**
     * Updates device type
     * @param deviceId device identifier
     * @param deviceType device type
     */
    suspend fun updateDeviceType(deviceId: Long, deviceType: Long) {
        val logMsg = "Updating device type for device: [${deviceId}] to [${deviceType}]"
        Timber.d(logMsg)
        val (index, device) = getIndexDevicePair(deviceId)
        if(index == null){
            val errorMsg = "Failed to update device type. Cause: Failed to read [${deviceId}]"
            Timber.e(errorMsg)
            return
        }

        val deviceBuilder = Device.newBuilder(device)
        deviceBuilder.deviceType = deviceType
        devicesDataStore.updateData { devices ->
            devices.toBuilder().setDevices(index, deviceBuilder.build()).build()
        }
    }

    /**
     * Updates device name
     *
     * @param deviceId device identifier
     * @param name device name
     */
    suspend fun updateDeviceName(deviceId: Long, name: String) {
        Timber.d("Updating device name for device: [${deviceId}]]")
        val (index, device) = getIndexDevicePair(deviceId)
        if(index == null){
            val errorMsg = "Failed to update device type. Cause: Failed to read [${deviceId}]"
            Timber.e(errorMsg)
            return
        }

        // update device name
        val deviceBuilder = Device.newBuilder(device)
        deviceBuilder.name = name
        devicesDataStore.updateData { devices ->
            devices.toBuilder().setDevices(index, deviceBuilder.build()).build()
        }
    }

    /**
     * Finds the index device pair for a give device identifier
     * @param deviceId device identifier
     * @return device index and device info pair
     */
    private suspend fun getIndexDevicePair(deviceId: Long) : Pair<Int?, Device?> {
        val devices = devicesFlow.first()
        val deviceCount = devices.devicesCount

        for (index in 0 until deviceCount) {
            val device = devices.getDevices(index)
            if(deviceId == device.deviceId){
                return index to device
            }
        }
        return null to null
    }

    /**
     * Finds the index for a given device identifier
     * @param deviceId device identifier
     * @return device index
     */
    private suspend fun getIndex(deviceId: Long): Int {
        val devices =  devicesFlow.first()
        val devicesCnt  =  devices.devicesCount
        for (index in 0 until devicesCnt){
            val device = devices.getDevices(index)
            if(deviceId == device.deviceId){
                return index
            }
        }
        return -1
    }

    /**
     * Returns the all the devices in the devices repo
     *
     * @return all the devices in the repo
     */
    suspend fun getAllDevices(): Devices {
        return devicesFlow.first()
    }

    /**
     * Removes device from repo
     *
     * @param deviceId device identifier
     */
    suspend fun removeDevice(deviceId: Long) {
        Timber.d("Deleting device with id : [${deviceId}]")
        val index =  getIndex(deviceId)
        if(index == -1){
            throw Exception("Device not found: $deviceId")
        }

        // remove device from data store
        devicesDataStore.updateData { deviceList ->
            deviceList.toBuilder().removeDevices(index).build()
        }
    }
}
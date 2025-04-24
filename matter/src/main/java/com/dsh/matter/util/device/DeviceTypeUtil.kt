package com.dsh.matter.util.device

import com.dsh.matter.model.device.DeviceType

object DeviceTypeUtil {

    /**
     * Device types map
     */
    private val typeMap = mapOf(
        DeviceType.Socket.type to DeviceType.Socket,
        DeviceType.ColorTemperatureLight.type to DeviceType.ColorTemperatureLight,
        DeviceType.OnOffLight.type to DeviceType.OnOffLight,
        DeviceType.ExtendedColorLight.type to DeviceType.ExtendedColorLight,
        DeviceType.DimmableLight.type to DeviceType.DimmableLight,
        DeviceType.Socket.type to DeviceType.Socket,
        DeviceType.DimmableSocket.type to DeviceType.DimmableSocket,
        DeviceType.OnOffLightSwitch.type to DeviceType.OnOffLightSwitch,
        DeviceType.DimmerSwitch.type to DeviceType.DimmerSwitch,
        DeviceType.ColorDimmerSwitch.type to DeviceType.ColorDimmerSwitch,
        DeviceType.Fan.type to DeviceType.Fan
    )

    /**
     * Converts long device type to enumerated value
     *
     * @param deviceType device type
     * @return the device type enum
     */
    @JvmStatic
    fun toEnum(deviceType: Long) : DeviceType {
        if(!typeMap.containsKey(deviceType)){
            val errorMsg = "Device type $deviceType is currently not supported"
            throw UnsupportedDeviceTypeException(errorMsg)
        }

        return typeMap[deviceType]!!
    }
}
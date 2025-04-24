package com.dsh.matter.util.device

import com.dsh.matter.model.device.FanMode

object FanModeUtil {

    /**
     * Fan modes map
     */
    private val modeMap = mapOf(
        FanMode.Off.mode to FanMode.Off,
        FanMode.Low.mode to FanMode.Low,
        FanMode.Medium.mode to FanMode.Medium,
        FanMode.High.mode to FanMode.High,
        FanMode.On.mode to FanMode.On,
        FanMode.Auto.mode to FanMode.Auto
    )

    /**
     * Converts int fan mode to enumerated value
     *
     * @param fanMode fan mode
     * @return the fan mode enum
     */
    @JvmStatic
    fun toEnum(fanMode: Int) : FanMode {
        if(!modeMap.containsKey(fanMode)){
            val errorMsg = "Fan mode type $fanMode is currently not supported"
            throw UnsupportedDeviceTypeException(errorMsg)
        }

        return modeMap[fanMode]!!
    }
}
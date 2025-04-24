package com.dsh.matter.util

import com.dsh.matter.model.device.DeviceStateAttributes
import com.dsh.matter.model.device.DeviceType
import com.dsh.matter.model.device.StateAttribute
import kotlin.math.roundToInt

internal object AttributeUtils {

    /**
     * Matter attributes max values
     */
    const val MTR_MAX_HUE = 254
    const val MTR_MAX_SATURATION = 254
    const val MTR_MAX_BRIGHTNESS = 254
    const val MTR_MIN_COLOR_TEMPERATURE = 154
    const val MTR_MAX_COLOR_TEMPERATURE = 526

    /**
     * Standard attributes max values
     */
    const val STD_MAX_HUE = 360
    const val STD_MAX_SATURATION = 100
    const val STD_MAX_BRIGHTNESS = 100
    const val STD_MIN_COLOR_TEMPERATURE = 0
    const val STD_MAX_COLOR_TEMPERATURE = 100

    /**
     * Remap attribute values
     *
     * This can be used to remap attribute values to different ranges.
     * Example: To convert the brightness value (0-255) into brightness percentage (0-100) and vice-versa.
     * @param value value to be mapped
     * @param from value's current max
     * @param to output value's range
     */
    fun mapValue(value : Int, from: Int, to: Int): Int {
        val mapped = (value * to) / (from * 1.0)
        return mapped.roundToInt()
    }

    /**
     * Remap attribute values
     *
     * This can be used to remap attribute values to different ranges.
     * @param value value to be mapped
     * @param valueMin input's min value
     * @param valueMax input's max value
     * @param resultMin result's min value
     * @param resultMax result's max value
     */
    fun mapValue(
        value: Int,
        valueMin: Int,
        valueMax: Int,
        resultMin: Int,
        resultMax: Int
    ): Int {
        val dividend = resultMax - resultMin
        val divisor = valueMax - valueMin
        val delta = value - valueMin
        return (delta * dividend + (divisor / 2)) / divisor + resultMin
    }

    /**
     * Checks if the [DeviceType] contains [StateAttribute]
     *
     * @param deviceType device type
     * @param stateAttribute state attribute
     */
    fun valStateAttribute(
        deviceType: DeviceType,
        stateAttribute: StateAttribute
    ): Boolean {
        val attributeSet = DeviceStateAttributes.map[deviceType] ?: return false
        return attributeSet.contains(stateAttribute)
    }
}
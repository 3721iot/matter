package com.dsh.matter.util

import android.content.Context
import android.graphics.Color
import com.dsh.matter.R
import com.dsh.matter.model.color.HSVColor
import timber.log.Timber

object ColorUtils {

    /**
     * Converts RGB hex color value to hsv
     *
     * @param rgbHex android color resource identifier[length 7]
     * @return the HSV color
     */
    @JvmStatic
    fun toHSV(rgbHex: String) : HSVColor? {
        if(rgbHex.isBlank()){
            throw IllegalArgumentException("Hex RGB args cannot be blank!")
        }

        if(rgbHex.length != 7 || !rgbHex.startsWith("#")){
            throw IllegalArgumentException("Arguments should be of length 7 and starts with #")
        }

        val colorData = rgbHex.substring(1, 7)
        val red = Integer.parseInt(colorData.substring(0, 2), 16)
        val green = Integer.parseInt(colorData.substring(2, 4), 16)
        val blue = Integer.parseInt(colorData.substring(4, 6),16)
        val hsv : FloatArray = floatArrayOf(0f, 0f, 0f)
        Color.RGBToHSV(red, green, blue, hsv)

        val hue = hsv[0].toInt()
        val saturation =  (hsv[1] * 100).toInt()
        val value =  (hsv[2] * 100).toInt()
        return HSVColor(hue = hue, saturation = saturation, value = value)
    }

    /**
     *  The list of supported device colors. This have to be replaced by an API call
     *
     *  @param context the application context
     *  @return the list of base colors
     */
    @JvmStatic
    fun getBaseDeviceColors(context: Context): List<Int> {
        val colorResourceList: MutableList<Int> = java.util.ArrayList()
        try {
            val colors = context.resources.obtainTypedArray(R.array.device_colors)
            for (i in 0 until colors.length()) {
                val color = colors.getColor(i, 0)
                colorResourceList.add(color)
            }
            colors.recycle()
        } catch (ex: Exception) {
            Timber.e("Failed to get colors. Cause: ${ex.localizedMessage}")
        }
        return colorResourceList
    }
}
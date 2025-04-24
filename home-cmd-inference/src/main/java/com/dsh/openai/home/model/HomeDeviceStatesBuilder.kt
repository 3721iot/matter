package com.dsh.openai.home.model

class HomeDeviceStatesBuilder {

    /**
     * The power status
     */
    private var power: Boolean? = null

    /**
     * The color temperature value
     */
    private var colorTemp: Int? = null

    /**
     * The brightness value
     */
    private var brightness: Int? = null

    /**
     * The HSV color value
     */
    private var color: MutableMap<String, Int> = mutableMapOf()

    /**
     * The power setter
     *
     * @param power the power value
     */
    fun setPower(power: Boolean): HomeDeviceStatesBuilder {
        this.power = power
        return this
    }

    /**
     * The color temperature setter
     *
     * @param temperature the color temperature value
     */
    fun setColorTemp(temperature: Int): HomeDeviceStatesBuilder {
        this.colorTemp = temperature
        return this
    }

    /**
     * The brightness setter
     *
     * @param brightness the brightness value
     */
    fun setBrightness(brightness: Int): HomeDeviceStatesBuilder {
        this.brightness = brightness
        return this
    }

    /**
     * The HSV color values setter
     *
     * @param hue the hue value
     * @param saturation the saturation value
     * @param value the brightness/value value
     */
    fun setColor(hue: Int, saturation: Int, value: Int): HomeDeviceStatesBuilder {
        this.color[HUE_KEY] = hue
        this.color[VALUE_KEY] = value
        this.color[SATURATION_KEY] = saturation
        return this
    }

    /**
     * the builder
     */
    fun build(): Map<String, Any> {
        val states = mutableMapOf<String, Any>()

        // sets the power
        if(null != this.power){
            states[POWER_KEY] = this.power!!
        }

        // sets the color
        if(color.isNotEmpty()) {
            states[HSV_COLOR_KEY] = this.color
        }

        // sets the color temperature
        if(null != this.colorTemp) {
            states[COLOR_TEMP_KEY] = this.colorTemp!!
        }

        // sets the brightness
        if(null != this.brightness){
            states[BRIGHTNESS_KEY] = this.brightness!!
        }

        return states.toMap()
    }

    companion object {

        /**
         * The hue key
         */
        private const val HUE_KEY = "h"

        /**
         * The value key
         */
        private const val VALUE_KEY = "v"

        /**
         * The power key
         */
        private const val POWER_KEY = "power"

        /**
         * The saturation key
         */
        private const val SATURATION_KEY = "s"

        /**
         * The HSV color key
         */
        private const val HSV_COLOR_KEY = "hsvColor"

        /**
         * The brightness key
         */
        private const val BRIGHTNESS_KEY = "brightness"

        /**
         * The color temperature key
         */
        private const val COLOR_TEMP_KEY = "colorTemperature"
    }
}
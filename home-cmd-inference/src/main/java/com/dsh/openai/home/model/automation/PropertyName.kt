package com.dsh.openai.home.model.automation
class PropertyName(val name: String) {
    companion object {
        /**
         * The weather property name
         */
        @JvmStatic
        val Weather: PropertyName = PropertyName("weather")

        /**
         * The humidity property name
         */
        @JvmStatic
        val Humidity: PropertyName = PropertyName("humidity")

        /**
         * The schedule property name
         */
        @JvmStatic
        val Schedule: PropertyName = PropertyName("schedule")

        /**
         * The wind-speed property name
         */
        @JvmStatic
        val WindSpeed: PropertyName = PropertyName("wind-speed")

        /**
         * The air quality property name
         */
        @JvmStatic
        val AirQuality: PropertyName = PropertyName("air-quality")

        /**
         * The temperature property name
         */
        @JvmStatic
        val Temperature: PropertyName = PropertyName("temperature")

        /**
         * The sunrise/sunset property name
         */
        @JvmStatic
        val SunsetSunrise: PropertyName = PropertyName("sunrise/sunset")

        /**
         * The device status change property name
         */
        @JvmStatic
        val DeviceStatusChange: PropertyName = PropertyName("device-status-change")
    }
}
package com.dsh.openai.home.model

enum class ControlIntent(val intent: String){
    /**
     * Power control
     */
    Power("power"),

    /**
     * Color control
     */
    Color("color"),

    /**
     * Brightness control
     */
    Brightness("brightness"),

    /**
     * Color temperature control
     */
    ColorTemperature("colorTemperature"),

    /**
     * Undefined control intent
     */
    Undefined("undefined")
}
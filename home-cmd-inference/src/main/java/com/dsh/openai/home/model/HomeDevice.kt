package com.dsh.openai.home.model

data class HomeDevice(
    /**
     * The device identifier
     */
    val deviceId: String,

    /**
     * The device name
     */
    val deviceName: String,

    /**
     * The device type
     */
    val deviceType: String,

    /**
     * The room to which the device is assigned
     */
    val roomName: String,

    /**
     * The home to which the device is assigned
     */
    val homeName: String,

    /**
     * The current device states
     */
    val deviceStates: Map<String, Any>
)

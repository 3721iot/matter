package com.dsh.openai.home.model.automation

data class DeviceStatusChangeValue(
    /**
     *  The target value
     */
    val targetValue: Any,

    /**
     * The device identifier
     */
    val deviceId: String,

    /**
     * The control intent
     */
    val controlIntent: String
)

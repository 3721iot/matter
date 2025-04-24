package com.dsh.openai.home.internal.model.device
import com.dsh.openai.home.model.ControlIntent

data class DeviceControlsArgs(
    /**
     * The device identifiers
     */
    val deviceIds: List<String>,
    /**
     * The control intent
     */
    val intent: ControlIntent,
    /**
     * The value
     */
    val value: String
)

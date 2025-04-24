package com.dsh.matter.model.device

/**
 * https://github.com/project-chip/connectedhomeip/blob/14ffac08583e5ee41c62270a2c4d58fea23eae1e
 * /examples/chef/sample_app_util/matter_device_types.json#L1
 */
enum class DeviceType(val type: Long) {
    /**
     * On/Off Light
     */
    OnOffLight(256),

    /**
     * Color Temperature Light
     */
    ColorTemperatureLight(268),

    /**
     * Extended Color Light
     */
    ExtendedColorLight(269),

    /**
     * Dimmable Light
     */
    DimmableLight(257),

    /**
     * On/Off Plug-In Unit
     */
    Socket(266),

    /**
     * Dimmable Plug-In Unit
     */
    DimmableSocket(267),

    /**
     * On/Off Light Switch
     */
    OnOffLightSwitch(259),

    /**
     * Dimmer Switch
     */
    DimmerSwitch(260),

    /**
     * Color Dimmer Switch
     */
    ColorDimmerSwitch(261),

    /**
     * Fan
     */
    Fan(43)
}
package com.dsh.matter.model.device

internal object DeviceStateAttributes {
    val map = mapOf(
        DeviceType.ColorTemperatureLight to setOf(
            StateAttribute.Online,
            StateAttribute.Switch,
            StateAttribute.ColorTemperature,
            StateAttribute.Brightness,
            StateAttribute.Color
        ),
        DeviceType.OnOffLight to setOf(
            StateAttribute.Online,
            StateAttribute.Switch,
            StateAttribute.ColorTemperature,
            StateAttribute.Brightness,
        ),

        DeviceType.Socket to setOf(
            StateAttribute.Online,
            StateAttribute.Switch,
        ),

        DeviceType.ExtendedColorLight to setOf(
            StateAttribute.Online,
            StateAttribute.Switch,
            StateAttribute.Brightness,
            StateAttribute.ColorTemperature,
            StateAttribute.Color
        ),

        DeviceType.OnOffLightSwitch to setOf(
            StateAttribute.Online,
            StateAttribute.Switch,
        )
    )
}
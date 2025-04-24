package com.dsh.matter.model.device

import com.dsh.matter.management.device.DeviceSubscriptionListener

internal data class DeviceStateChangeTask(
    var timeouts: Int = 0,
    val listener: DeviceSubscriptionListener
)

package com.dsh.matter.model.scanner

data class DeviceSharePayload(
    val qrCode: String,
    val manualCode: String,
    val setupCode: Long,
    val discriminator: Int
)

package com.dsh.tether.model

data class MtrDeviceMetadata(
    val vendorId: Int = 0,
    val productId: Int = 0,
    val version: Int = 0,
    val discriminator: Int = 0,
    val setupPinCode: Long = 0L,
    var commissioningFlow: Int = 0,
    val hasShortDiscriminator: Boolean = false,
    val discoveryCapabilities: HashSet<Int> = HashSet()
)

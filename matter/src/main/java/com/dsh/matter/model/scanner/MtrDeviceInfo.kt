package com.dsh.matter.model.scanner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import chip.setuppayload.DiscoveryCapability

@Parcelize
data class MtrDeviceInfo(
    val version: Int = 0,
    var vendorId: Int = 0,
    var productId: Int = 0,
    val discriminator: Int = 0,
    val setupPinCode: Long = 0L,
    var commissioningFlow: Int = 0,
    val optionalQrCodeInfoMap: Map<Int, MtrQrCodeInfo> = mapOf(),
    val discoveryCapabilities: Set<DiscoveryCapability> = setOf(),
    val hasShortDiscriminator: Boolean = false,
    var ipAddress: String? = null,
) : Parcelable

package com.dsh.matter.model.scanner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SetupPayloadDescriptor(
    val version: Int = 0,
    val discriminator: Int = 0,
    val setupPinCode: Long = 0L,
    var commissioningFlow: Int = 0,
    val hasShortDiscriminator: Boolean = false,
    val discoveryCapabilities: HashSet<Int> = HashSet()
) : Parcelable

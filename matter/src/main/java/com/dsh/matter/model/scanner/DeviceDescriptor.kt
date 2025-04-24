package com.dsh.matter.model.scanner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeviceDescriptor(
    val vendorId: Int = 0,
    val productId: Int = 0,
    val deviceType: Long = 0
) : Parcelable

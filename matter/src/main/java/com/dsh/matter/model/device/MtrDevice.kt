package com.dsh.matter.model.device

import android.os.Parcelable
import com.dsh.matter.model.scanner.DeviceDescriptor
import com.dsh.matter.model.scanner.SetupPayloadDescriptor
import kotlinx.parcelize.Parcelize

@Parcelize
data class MtrDevice(
    val id: Long,
    var name: String,
    val room: String,
    val home: String,
    val deviceDescriptor: DeviceDescriptor,
    val setupPayloadDescriptor: SetupPayloadDescriptor
): Parcelable

package com.dsh.matter.model.scanner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import chip.setuppayload.OptionalQRCodeInfo.OptionalQRCodeInfoType

@Parcelize
data class MtrQrCodeInfo (
    val tag: Int,
    val type: OptionalQRCodeInfoType,
    val data: String,
    val intDataValue: Int
): Parcelable


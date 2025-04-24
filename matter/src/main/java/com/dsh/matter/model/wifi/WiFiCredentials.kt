package com.dsh.matter.model.wifi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WiFiCredentials(val ssid: String, val password: String) : Parcelable

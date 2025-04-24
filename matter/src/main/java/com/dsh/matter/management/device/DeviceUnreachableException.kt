package com.dsh.matter.management.device

class DeviceUnreachableException(
    message: String? = null, cause: Throwable? = null
) : Exception(message, cause)
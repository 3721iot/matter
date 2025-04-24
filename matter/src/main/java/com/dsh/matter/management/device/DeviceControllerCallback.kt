package com.dsh.matter.management.device

import java.lang.Exception

abstract class DeviceControllerCallback {

    /**
     * Invoked on device control success
     */
    abstract fun onSuccess()

    /**
     * Invoked on device control failure
     */
    abstract fun onError(error: Exception?)
}
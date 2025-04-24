package com.dsh.matter.management.device

abstract class DeviceDecommissionCallback {

    /**
     * Invoked on device decommissioning error
     *
     * @param code the error code
     * @param message the error message
     */
    abstract fun onError(code: Int, message: String)

    /**
     * Invoked on a successful device decommission
     * @param deviceId the device identifier
     */
    abstract fun onSuccess(deviceId: Long)
}
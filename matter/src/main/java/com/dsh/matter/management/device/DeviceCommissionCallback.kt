package com.dsh.matter.management.device

import com.dsh.matter.model.CommissioningErrorCode
import com.dsh.matter.model.device.MtrDevice

abstract class DeviceCommissionCallback {
    /**
     * Invoked on commissioning error
     * @param code the commissioning error code
     * @param message the commissioning error message
     */
    abstract fun onError(code: CommissioningErrorCode, message: String? = null)

    /**
     * Invoked on attestation failure
     * @param code the commissioning error code
     * @param devicePtr the device pointer
     */
    abstract fun onAttestationFailure(code: CommissioningErrorCode, devicePtr: Long)

    /**
     * Invoked on commissioning success.
     *
     * @param mtrDevice the commissioned matter device
     */
    abstract fun onSuccess(mtrDevice: MtrDevice)

    /**
     * Invoked when WiFi credentials are required
     * @param devicePtr the device pointer
     */
    abstract fun onWiFiCredentialsRequired(devicePtr: Long)
}
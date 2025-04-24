package com.dsh.matter.commission.provisioning

internal abstract class DeviceAttestationCallback {

    /**
     *  Invoked when attestation fail
     *
     *  @param devicePtr device pointer
     *  @param errorCode error code
     */
    abstract fun onFailure(devicePtr: Long, errorCode: Int)
}


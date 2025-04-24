package com.dsh.matter.commission

import chip.devicecontroller.ChipDeviceController
import timber.log.Timber

internal open class MtrCompletionListener : ChipDeviceController.CompletionListener {
    override fun onConnectDeviceComplete() {
        Timber.d("onConnectDeviceComplete()")
    }

    override fun onStatusUpdate(status: Int) {
        Timber.d("onStatusUpdate(status: Int)[${status}]")
    }

    override fun onPairingComplete(errorCode: Int) {
        Timber.d("onPairingComplete(errorCode: Int)[${errorCode}]")
    }

    override fun onPairingDeleted(errorCode: Int) {
        Timber.d("onPairingDeleted(errorCode: Int)[${errorCode}]")
    }

    override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
        val logMsg ="onCommissioningComplete(nodeId: Long, errorCode: Int)[${nodeId}, ${errorCode}]"
        Timber.d(logMsg)
    }

    override fun onReadCommissioningInfo(
        vendorId: Int,
        productId: Int,
        wifiEndpointId: Int,
        threadEndpointId: Int
    ) {
        val logMsg =" onReadCommissioningInfo(\n" +
                "        vendorId: Int,\n" +
                "        productId: Int,\n" +
                "        wifiEndpointId: Int,\n" +
                "        threadEndpointId: Int\n" +
                "    )[${vendorId}, ${productId}, ${wifiEndpointId}, ${threadEndpointId}]"
        Timber.d(logMsg)
    }

    override fun onCommissioningStatusUpdate(nodeId: Long, stage: String?, errorCode: Int) {
        val logMsg ="onCommissioningStatusUpdate(" +
                "nodeId: Long, stage: String?, errorCode: Int" +
                ")[${nodeId}, $stage, ${errorCode}]"
        Timber.d(logMsg)
    }

    override fun onNotifyChipConnectionClosed() {
        Timber.d("onNotifyChipConnectionClosed()")
    }

    override fun onCloseBleComplete() {
        Timber.d("onCloseBleComplete()")
    }

    override fun onError(error: Throwable?) {
        Timber.d("onError(error: Throwable?)[${error?.localizedMessage}]")
    }

    override fun onOpCSRGenerationComplete(csr: ByteArray?) {
        Timber.d("onOpCSRGenerationComplete(csr: ByteArray?)[${csr}]")
    }
}
package com.dsh.matter.commission.provisioning

import com.dsh.matter.commission.MtrCompletionListener

internal open class CommissioningListener : MtrCompletionListener() {

    /**
     *  Invoked when commission is completed
     *
     *  @param nodeId node/device identifier
     *  @param errorCode error code
     */
    override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
    }


    /**
     * Invoked when device pairing is completed
     *
     * @param errorCode error code
     */
    override fun onPairingComplete(errorCode: Int) {
    }


    /**
     * Invoked in the event of an error
     *
     * @param error throwable exception
     */
    override fun onError(error: Throwable?) {
    }
}
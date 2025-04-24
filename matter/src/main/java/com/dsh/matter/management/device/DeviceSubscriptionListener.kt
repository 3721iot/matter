package com.dsh.matter.management.device

import com.dsh.matter.model.device.StateAttribute

abstract class DeviceSubscriptionListener {

    /**
     * Invoked on device subscription error
     * @param ex the error exception
     */
    abstract fun onError(ex: Exception)

    /**
     * Invoked when device states subscription is established
     * @param subscriptionId the subscription identifier
     */
    abstract fun onSubscriptionEstablished(subscriptionId: ULong)

    /**
     * Invoked when a new report is received
     * @param report the device states report
     */
    abstract fun onReport(report: HashMap<StateAttribute, Any>)
}
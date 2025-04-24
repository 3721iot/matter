package com.dsh.matter.util

import chip.setuppayload.DiscoveryCapability
import com.dsh.matter.model.scanner.DeviceDescriptor
import com.dsh.matter.model.scanner.MtrDeviceInfo
import com.dsh.matter.model.scanner.SetupPayloadDescriptor
import com.dsh.matter.model.device.MtrDevice

class MtrDeviceBuilder {

    /**
     * The device identifier
     */
    private var deviceId: Long? = null

    /**
     * The device type
     */
    private var deviceType: Long? = null

    /**
     * The device info
     */
    private var deviceInfo: MtrDeviceInfo? = null

    /**
     * Sets the device identifier
     *
     * @param deviceId device identifier
     * @return the instance of the builder
     */
    fun setDeviceId(deviceId: Long): MtrDeviceBuilder{
        this.deviceId = deviceId
        return this
    }

    /**
     * Sets the device type
     *
     * @param deviceType device type
     * @return the instance of the builder
     */
    fun setDeviceType(deviceType: Long): MtrDeviceBuilder {
        this.deviceType = deviceType
        return this
    }

    /**
     * Sets the device information
     *
     * @param deviceInfo device information
     */
    fun setDeviceInfo(deviceInfo: MtrDeviceInfo): MtrDeviceBuilder {
        this.deviceInfo = deviceInfo
        return this
    }

    /**
     * Builds device descriptor
     *
     * @param mtrDeviceInfo device information
     * @param deviceType device type
     * @return the device descriptor
     */
    private fun buildDeviceDescriptor(
        mtrDeviceInfo: MtrDeviceInfo,
        deviceType: Long
    ): DeviceDescriptor {
        return DeviceDescriptor(
            vendorId = mtrDeviceInfo.vendorId,
            productId = mtrDeviceInfo.productId,
            deviceType = deviceType
        )
    }

    /**
     * Build the setup descriptor from the device info
     *
     * @param mtrDeviceInfo device information
     * @return the setup payload descriptor
     */
    private fun buildSetupPayloadDescriptor(
        mtrDeviceInfo: MtrDeviceInfo
    ): SetupPayloadDescriptor {
        val discoveryCapabilities = HashSet<Int>()
        mtrDeviceInfo.discoveryCapabilities.forEach {
            when(it){
                DiscoveryCapability.SOFT_AP ->{
                    discoveryCapabilities.add(0)
                }
                DiscoveryCapability.BLE ->{
                    discoveryCapabilities.add(1)
                }
                DiscoveryCapability.ON_NETWORK ->{
                    discoveryCapabilities.add(2)
                }
            }
        }

        return SetupPayloadDescriptor(
            version = mtrDeviceInfo.version,
            discriminator = mtrDeviceInfo.discriminator,
            setupPinCode = mtrDeviceInfo.setupPinCode,
            commissioningFlow = mtrDeviceInfo.commissioningFlow,
            hasShortDiscriminator = mtrDeviceInfo.hasShortDiscriminator,
            discoveryCapabilities = discoveryCapabilities
        )
    }

    /**
     * Builds the Matter device
     * @return the matter device
     */
    fun build(): MtrDevice {
        // ToDo() check if everything is in place
        return  MtrDevice(
            id = this.deviceId!!,
            name = "Matter Device",
            room = "Attic",
            home = "Someone",
            deviceDescriptor = buildDeviceDescriptor(this.deviceInfo!!, this.deviceType!!),
            setupPayloadDescriptor = buildSetupPayloadDescriptor(this.deviceInfo!!)
        )
    }
}
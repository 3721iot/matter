package com.dsh.matter.util.scanner

import chip.setuppayload.DiscoveryCapability
import chip.setuppayload.SetupPayload
import chip.setuppayload.SetupPayloadParser
import com.dsh.matter.model.scanner.InvalidEntryCodeException
import com.dsh.matter.model.scanner.InvalidQrCodeException
import com.dsh.matter.model.scanner.InvalidSetupCodeException
import com.dsh.matter.model.scanner.MtrDeviceInfo
import com.dsh.matter.model.scanner.MtrQrCodeInfo
import com.dsh.matter.model.scanner.SetupPayloadDescriptor

object SetupPayloadUtil {

    /**
     * Extracts the setup payload data into a matter device info
     *
     * @param setupPayload the setup payload
     * @return the matter device info object
     */
    @JvmStatic
    fun toMtrDeviceInfo(setupPayload: SetupPayload): MtrDeviceInfo {
        return MtrDeviceInfo(
            version = setupPayload.version,
            vendorId = setupPayload.vendorId,
            productId = setupPayload.productId,
            discriminator = setupPayload.discriminator,
            setupPinCode = setupPayload.setupPinCode,
            commissioningFlow = setupPayload.commissioningFlow,
            optionalQrCodeInfoMap = setupPayload.optionalQRCodeInfo.mapValues { (_ , info) ->
                MtrQrCodeInfo(
                    tag = info.tag,
                    type = info.type,
                    data = info.data,
                    intDataValue = info.int32
                )
            },
            discoveryCapabilities = setupPayload.discoveryCapabilities,
            hasShortDiscriminator = setupPayload.hasShortDiscriminator
        )
    }

    /**
     * Converts qrcode and or or manual payload to matter device info
     *
     * @param qrCodePayload the qr code payload
     * @param manualCodePayload the manual code payload
     * @return the matter device info
     */
    @JvmStatic
    fun toMtrDeviceInfo(qrCodePayload: String?, manualCodePayload: String?): MtrDeviceInfo? {
        if(null != qrCodePayload){
            val payload : SetupPayload = SetupPayloadParser().parseQrCode(qrCodePayload)
            return toMtrDeviceInfo(payload)
        }else if(null != manualCodePayload){
            val payload : SetupPayload = SetupPayloadParser().parseManualEntryCode(manualCodePayload)
            return toMtrDeviceInfo(payload)
        }
        return null
    }

    /**
     * Checks if a QR code is a Matter code
     *
     * @param value QR code
     */
    @JvmStatic
    fun isMatterQrCode(value: String): Boolean {
        return value.matches(Regex("""MT:[A-Z\d.-]{19,}"""))
    }

    /**
     * Build the setup descriptor from the setup payload
     *
     * @param payload the device payload
     * @return the setup payload descriptor
     */
    private fun buildSetupPayloadDescriptor(
        payload: SetupPayload
    ): SetupPayloadDescriptor {
        val discoveryCapabilities = HashSet<Int>()
        payload.discoveryCapabilities.forEach {
            when (it) {
                DiscoveryCapability.SOFT_AP -> {
                    discoveryCapabilities.add(0)
                }

                DiscoveryCapability.BLE -> {
                    discoveryCapabilities.add(1)
                }

                DiscoveryCapability.ON_NETWORK -> {
                    discoveryCapabilities.add(2)
                }

                else -> {
                    discoveryCapabilities.add(0)
                }
            }
        }

        return SetupPayloadDescriptor(
            version = payload.version,
            discriminator = payload.discriminator,
            setupPinCode = payload.setupPinCode,
            commissioningFlow = payload.commissioningFlow,
            hasShortDiscriminator = payload.hasShortDiscriminator,
            discoveryCapabilities = discoveryCapabilities
        )
    }

    /**
     * Generates a setup payload descriptor from matter onboarding payload
     * @param onBoardingPayload matter onboarding payload
     * @return the setup payload descriptor
     */
    @JvmStatic
    fun getSetupPayloadDescriptor(onBoardingPayload: String): SetupPayloadDescriptor {
        try {
            val payload : SetupPayload = if(isMatterQrCode(onBoardingPayload)){
                SetupPayloadParser().parseQrCode(onBoardingPayload)
            }else{
                SetupPayloadParser().parseManualEntryCode(onBoardingPayload)
            }
            return buildSetupPayloadDescriptor(payload)
        }catch (ex: SetupPayloadParser.UnrecognizedQrCodeException){
            throw InvalidQrCodeException("Unrecognized qr code")
        }catch (ex: SetupPayloadParser.SetupPayloadException) {
            throw InvalidSetupCodeException("Invalid on-boarding payload")
        }catch (ex: SetupPayloadParser.InvalidEntryCodeFormatException){
            throw InvalidEntryCodeException("Invalid entry code")
        }catch (ex: Exception){
            throw Exception(ex.localizedMessage)
        }
    }
}
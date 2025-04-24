package com.dsh.matter.management.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.content.Context
import chip.devicecontroller.*
import chip.setuppayload.DiscoveryCapability
import chip.setuppayload.SetupPayload
import chip.setuppayload.SetupPayloadParser
import com.dsh.matter.MtrClient
import com.dsh.matter.commission.bluetooth.BluetoothManager
import com.dsh.matter.commission.provisioning.CommissioningListener
import com.dsh.matter.management.cluster.ClusterManager
import com.dsh.matter.model.CommissioningErrorCode
import com.dsh.matter.model.scanner.*
import com.dsh.matter.model.wifi.WiFiCredentials
import com.dsh.matter.util.MtrDeviceBuilder
import com.dsh.matter.util.scanner.SetupPayloadUtil
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

/**
 * Facilitates with device management ops
 */
@Singleton
class DeviceManager @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Coroutine scope
     */
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /**
     * Initialize client, cluster manager and device cache repo
     */
    private var clusterManager: ClusterManager = ClusterManager(context)

    /**
     * Ble GATT
     */
    private var bluetoothGatt: BluetoothGatt? = null

    /**
     * Current device identifier
     */
    private var currentDeviceId: Long? = null

    /**
     * Current device ble connection identifier
     */
    private var currentBleConnId: Int ? = null

    /**
     * Current device information
     */
    private var currentDeviceInfo: MtrDeviceInfo? = null

    /**
     * Device commissioning callback
     */
    private var deviceCommissioningCallback: DeviceCommissionCallback? = null

    /**
     * Whether commissioning is in progress or not
     */
    private var isInProgress = false

    /**
     * Cached SecureRandom instance
     */
    private val secureRandom: SecureRandom by lazy {
        try {
            SecureRandom.getInstance("SHA1PRNG")
        } catch (ex: Exception) {
            Timber.w(ex, "Failed to instantiate SecureRandom with SHA1PRNG")
            SecureRandom()
        }
    }

    /**
     * Removes the device from Matter fabric
     *
     * @param deviceId device identifier
     * @param callback decommission callback
     */
    fun decommissionDevice(
        deviceId: Long, callback: DeviceDecommissionCallback
    ){
        coroutineScope.launch {
            val unpairDeviceCallback = object : UnpairDeviceCallback {
                override fun onError(status: Int, remoteDeviceId: Long) {
                    Timber.e("onError()[$status, $remoteDeviceId]")
                    val errorMsg = "Failed to decommission device: $deviceId"
                    callback.onError(status, errorMsg)
                }

                override fun onSuccess(remoteDeviceId: Long) {
                    callback.onSuccess(deviceId)
                }
            }
            MtrClient.getDeviceController(context).unpairDeviceCallback(deviceId, unpairDeviceCallback)
        }
    }

    /**
     * Checks if a QR code is a Matter code
     *
     * @param value QR code
     */
    private fun isMatterQrCode(value: String): Boolean {
        return value.matches(Regex("""MT:[A-Z\d.-]{19,}"""))
    }

    /**
     * Retrieves the setup payload from the passed on-boarding payload
     *
     * @param onBoardingPayload on-boarding payload
     */
    private fun getSetupPayload(onBoardingPayload: String) : SetupPayload{
        try {
            val payload : SetupPayload = if(isMatterQrCode(onBoardingPayload)){
                SetupPayloadParser().parseQrCode(onBoardingPayload)
            }else{
                SetupPayloadParser().parseManualEntryCode(onBoardingPayload)
            }
            return payload
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

    /***
     * Commission device
     *
     * @param onBoardingPayload on-boarding payload
     * @param callback Commissioning callback
     */
    fun startCommissioningDevice(
        onBoardingPayload: String,
        callback: DeviceCommissionCallback
    ) {
        if(isInProgress){
            // Stop current commissioning task
            finishCommissioningDevice()
        }

        // set commission progress flag
        isInProgress = true

        // Initialize the callback
        deviceCommissioningCallback = callback

        // Extract matter device info from QR code
        val payload = try {
            getSetupPayload(onBoardingPayload)
        }catch (ex: Exception) {
            callback.onError(CommissioningErrorCode.InvalidOnBoardingPayload, ex.message)
            return
        }

        Timber.d("The payload: [${Gson().toJson(payload)}]")
        val deviceInfo = SetupPayloadUtil.toMtrDeviceInfo(payload)
        Timber.d("The device info: [${Gson().toJson(deviceInfo)}]")
        currentDeviceInfo = deviceInfo

        // get device identifiers
        coroutineScope.launch {
            currentDeviceId = generateNextDeviceId()
            Timber.d("Current device ID: $currentDeviceId")
            if(deviceInfo.discoveryCapabilities.contains(DiscoveryCapability.ON_NETWORK)){
                // No WiFi credentials are required for this
                commissionViaUdp()
            }else if(deviceInfo.discoveryCapabilities.contains(DiscoveryCapability.BLE)){
                // Go through the with the WiFi route
                deviceCommissioningCallback?.onWiFiCredentialsRequired(currentDeviceId!!)
            } else{
                // Take the unknown route
                commissionViaBluetoothWithFallback()
            }
        }
    }

    /***
     * Commission device
     *
     * @param deviceId the matter device id.
     * @param onBoardingPayload on-boarding payload.
     * @param callback Commissioning callback.
     */
    fun startCommissioningDevice(
        deviceId: Long,
        onBoardingPayload: String,
        callback: DeviceCommissionCallback
    ) {
        if(isInProgress){
            // Stop current commissioning task
            finishCommissioningDevice()
        }

        // check device identifier
        if(deviceId == 0L){
            callback.onError(
                CommissioningErrorCode.InvalidDeviceIdentifier, "Invalid device identifier"
            )
        }

        // set commission progress flag
        isInProgress = true

        // Initialize the callback
        deviceCommissioningCallback = callback

        // Extract matter device info from QR code
        val payload = try {
            getSetupPayload(onBoardingPayload)
        }catch (ex: Exception) {
            callback.onError(CommissioningErrorCode.InvalidOnBoardingPayload, ex.message)
            return
        }

        val deviceInfo = SetupPayloadUtil.toMtrDeviceInfo(payload)
        currentDeviceInfo = deviceInfo

        // get device identifiers
        coroutineScope.launch {
            currentDeviceId = deviceId
            Timber.d("Current device ID: $currentDeviceId")
            if(deviceInfo.discoveryCapabilities.contains(DiscoveryCapability.ON_NETWORK)){
                // No WiFi credentials are required for this
                commissionViaUdp()
            }else if(deviceInfo.discoveryCapabilities.contains(DiscoveryCapability.BLE)){
                // Go through the with the WiFi route
                deviceCommissioningCallback?.onWiFiCredentialsRequired(currentDeviceId!!)
            } else{
                // Take the unknown route
                commissionViaBluetoothWithFallback()
            }
        }
    }

    /**
     * Commission device via BLE with UDP fallback option
     */
    private fun commissionViaBluetoothWithFallback() {
        coroutineScope.launch {
            val bluetoothManager = BluetoothManager(context)
            val device = bluetoothManager.getBleDevice(
                currentDeviceInfo?.discriminator!!, currentDeviceInfo?.hasShortDiscriminator!!
            )

            if(null == device){
                commissionViaUdpFallback()
            }else{
                // Connecting to device
                Timber.d("Connecting to device ${device.address}")
                bluetoothGatt = bluetoothManager.connectDevice(context, device)

                // get device identifiers
                currentBleConnId = bluetoothManager.connectedId
                Timber.d("Bluetooth connection ID: $currentBleConnId")
                // set device commissioning listener
                Timber.d("Pairing device ${device.address}")
                // set commission completion listener
                val completionCallback = getCommissionCompletionCallback()
                MtrClient.getDeviceController(context).setCompletionListener(completionCallback)
                // request WiFi credentials
                Timber.d("Requesting WiFi credentials")
                deviceCommissioningCallback?.onWiFiCredentialsRequired(currentDeviceId!!)
            }
        }
    }

    /**
     * Generates a random number to be used as a device identifier during device commissioning
     *
     * @return the device identifier
     */
    private fun generateNextDeviceId(): Long {
        return secureRandom.nextLong().takeIf { it != Long.MIN_VALUE }?.let {
            if (it < 0) {
                // Making sure the generated number is always positive
                -it
            } else {
                // Adding 1 to make sure the generated number is greater than 0
                it + 1
            }
        } ?: 1
    }

    /**
     * Continues commissioning device via bluetooth
     * @param credentials WiFi credentials
     */
    private fun continueCommissioningViaBluetooth(credentials: WiFiCredentials){
        Timber.d("Commissioning over bluetooth")
        coroutineScope.launch {
            // set network credentials
            val networkCredentials =
                NetworkCredentials.forWiFi(
                    NetworkCredentials.WiFiCredentials(credentials.ssid, credentials.password)
                )

            // Set device attestation
            setupDeviceAttestationDelegate()

            try {
                // Commission device
                MtrClient.getDeviceController(context).pairDevice(
                    bluetoothGatt,
                    currentBleConnId!!,
                    currentDeviceId!!,
                    currentDeviceInfo?.setupPinCode!!,
                    networkCredentials
                )
            }catch (ex: Exception) {
                Timber.e("Commissioning failed. Cause: ${ex.localizedMessage}")
                deviceCommissioningCallback?.onError(
                    CommissioningErrorCode.Unknown,
                    "Failed to pair the device."
                )
            }
        }
    }

    /**
     * Commission device via Bluetooth
     *
     * @param credentials WiFi credentials
     */
    private fun commissionViaBluetooth(
        credentials: WiFiCredentials
    ){
        coroutineScope.launch {
            val bluetoothManager = BluetoothManager(context)
            val device =
                bluetoothManager.getBleDevice(
                    currentDeviceInfo?.discriminator!!, currentDeviceInfo?.hasShortDiscriminator!!
                ) ?: run {
                        val errorMsg = "Make sure the device is powered on and try again"
                        Timber.d(errorMsg)
                        deviceCommissioningCallback?.onError(
                            CommissioningErrorCode.DeviceNotFound,
                            "Ble manager failed to find device"
                        )
                        finishCommissioningDevice()
                        return@launch
                    }

            // Connecting to device
            Timber.d("Connecting to device ${device.address}")
            bluetoothGatt = bluetoothManager.connectDevice(context, device)

            // set device commissioning listener
            Timber.d("Pairing device ${device.address}")
            val completionCallback = getCommissionCompletionCallback()
            MtrClient.getDeviceController(context).setCompletionListener(completionCallback)

            // get device identifiers
            val bleConnId = bluetoothManager.connectedId
            Timber.d("Bluetooth connection ID: $bleConnId")

            // set network credentials
            val networkCredentials =
                NetworkCredentials.forWiFi(
                    NetworkCredentials.WiFiCredentials(credentials.ssid, credentials.password)
                )

            // Set device attestation
            setupDeviceAttestationDelegate()

            try {
                // Commission device
                MtrClient.getDeviceController(context).pairDevice(
                    bluetoothGatt,
                    bleConnId,
                    currentDeviceId!!,
                    currentDeviceInfo?.setupPinCode!!,
                    networkCredentials
                )
            }catch (ex: Exception) {
                Timber.e("Commissioning failed. Cause: ${ex.localizedMessage}")
                deviceCommissioningCallback?.onError(
                    CommissioningErrorCode.Unknown,
                    "Failed to pair the device."
                )
            }
        }
    }

    /**
     * Commission device via UDP with BLE fallback option
     */
    private fun commissionViaUdpFallback() {
        // Discover nodes with open windows
        MtrClient.getDeviceController(context).discoverCommissionableNodes()

        coroutineScope.launch {
            // Give the function room to do its thing
            delay(DEVICE_DISCOVERY_TIMEOUT_MS)

            // Process discovered nodes
            for(index in 0..10) {
                val device = MtrClient.getDeviceController(context).getDiscoveredDevice(index) ?: continue
                Timber.d("Discovered device: discriminator=${device.discriminator} | ip address= ${device.ipAddress}")
                // set device address
                currentDeviceInfo?.ipAddress = device.ipAddress
                break
            }

            // Check if an IP address has been assigned to the device
            if(null == currentDeviceInfo?.ipAddress) {
                Timber.d("Nothing on UDP. It's time to call out the big guns")
                deviceCommissioningCallback?.onWiFiCredentialsRequired(currentDeviceId!!)
                return@launch
            }

            // set device commissioning listener
            Timber.d("Pairing device ${currentDeviceInfo?.ipAddress}")
            val completionCallback = getCommissionCompletionCallback()
            MtrClient.getDeviceController(context).setCompletionListener(completionCallback)

            // Set device attestation
            setupDeviceAttestationDelegate()

            try {
                // Commission device
                MtrClient.getDeviceController(context).pairDeviceWithAddress(
                    currentDeviceId!!,
                    currentDeviceInfo?.ipAddress, DEVICE_IANA_PORT,
                    currentDeviceInfo?.discriminator!!,
                    currentDeviceInfo?.setupPinCode!!,
                    null
                )
            }catch (ex: Exception) {
                Timber.e("Commissioning failed. Cause: ${ex.localizedMessage}")
                deviceCommissioningCallback?.onError(
                    CommissioningErrorCode.Unknown,
                    "Failed to pair the device."
                )
            }
        }
    }

    /**
     * Commission device via UDP
     */
    private fun commissionViaUdp(){
        // Discover nodes with open windows
        MtrClient.getDeviceController(context).discoverCommissionableNodes()

        coroutineScope.launch{
            // Give the function room to do its thing
            delay(DEVICE_DISCOVERY_TIMEOUT_MS)

            // Process discovered nodes
            for(index in 0..10) {
                val device = MtrClient.getDeviceController(context).getDiscoveredDevice(index) ?: continue
                Timber.d("Discovered device: discriminator=${device.discriminator} | ip address= ${device.ipAddress}")
                if (device.discriminator.toInt() != currentDeviceInfo?.discriminator){
                    continue
                }

                // set device address
                currentDeviceInfo?.ipAddress =  device.ipAddress
                break
            }

            // Check if an IP address has been assigned to the device
            if(currentDeviceInfo?.ipAddress == null) {
                val errorMsg = "Make sure the device owner has started device sharing"
                Timber.d(errorMsg)
                Timber.d(errorMsg)
                deviceCommissioningCallback?.onError(
                    CommissioningErrorCode.DeviceNotFound,
                    "Failed to discover device node"
                )
                finishCommissioningDevice()
                return@launch
            }

            // set device commissioning listener
            Timber.d("Pairing device ${currentDeviceInfo?.ipAddress}")
            val completionCallback = getCommissionCompletionCallback()
            MtrClient.getDeviceController(context).setCompletionListener(completionCallback)

            // Set device attestation
            setupDeviceAttestationDelegate()

            try {
                // Commission device
                MtrClient.getDeviceController(context).pairDeviceWithAddress(
                    currentDeviceId!!,
                    currentDeviceInfo?.ipAddress, DEVICE_IANA_PORT,
                    currentDeviceInfo?.discriminator!!,
                    currentDeviceInfo?.setupPinCode!!,
                    null
                )
            }catch (ex: Exception) {
                Timber.e("Commissioning failed. Cause: ${ex.localizedMessage}")
                deviceCommissioningCallback?.onError(
                    CommissioningErrorCode.Unknown,
                    "Failed to pair the device."
                )
            }
        }
    }

    /**
     * Continues device commissioning with WiFi credentials
     *
     * @param deviceId device identifier
     * @param wifiCredentials wifi credentials
     */
    fun continueCommissioningDevice(
        deviceId: Long, wifiCredentials: WiFiCredentials
    ) {
        if(currentDeviceId != deviceId){
            deviceCommissioningCallback
                ?.onError(
                    CommissioningErrorCode.PairingFailed,
                    "Invalid device data"
                )
            finishCommissioningDevice()
        }

        Timber.d("Ble conn identifier")
        if(null != currentBleConnId){
            continueCommissioningViaBluetooth(wifiCredentials)
        }else{
            commissionViaBluetooth(wifiCredentials)
        }
    }

    /**
     * Continues device commissioning after a failed attestation
     *
     * @param devicePtr device pointer
     * @param ignoreFailure whether to ignore failure or not
     */
    fun continueCommissioningDevice(
        devicePtr: Long, ignoreFailure: Boolean
    ) {
        coroutineScope.launch {
            Timber.d("Continuing device commissioning")
            MtrClient.getDeviceController(context).continueCommissioning(devicePtr, ignoreFailure)
        }
    }

    /**
     * Stops device commissioning process
     */
    @SuppressLint("MissingPermission")
    fun finishCommissioningDevice() {
        coroutineScope.launch {
            try {
                // reset connected device id/pointer, its payload info and associated callback
                currentDeviceId = 0
                currentBleConnId = null
                currentDeviceInfo = null
                deviceCommissioningCallback = null
                // clean up the device controller
                MtrClient.getDeviceController(context).close()
            }catch (ex: Exception){
                Timber.e("Failed to wrap thing up.", ex)
            }
            isInProgress = false
        }
    }

    /**
     * Sets up device attestation delegate/handler
     */
    private fun setupDeviceAttestationDelegate(){
        MtrClient.getDeviceController(context).setDeviceAttestationDelegate(
            ATTESTATION_TIMEOUT
        ) { devicePtr, _, errorCode ->
            Timber.d(
                "Device attestation: devicePtr=${devicePtr} errorCode=${errorCode}"
            )

            // ToDo() bubble failures to the app module
            // Ignoring all attestation failures and continue commissioning
            coroutineScope.launch {
                Timber.d("Continuing device commissioning")
                MtrClient.getDeviceController(context)
                    .continueCommissioning(devicePtr, true)
            }
        }
    }

    /**
     * Generates a commission completion callback
     */
    private fun getCommissionCompletionCallback(): CommissioningListener {
        return object : CommissioningListener() {
            override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
                super.onCommissioningComplete(nodeId, errorCode)
                coroutineScope.launch {
                    Timber.d("Commissioning completed for node $nodeId with code: $errorCode")
                    if (errorCode == 0) {
                        onCommissioningSuccess(deviceId = nodeId)
                    } else {
                        val message = "Commissioning failed with code: $errorCode"
                        Timber.d(message)
                        deviceCommissioningCallback?.onError(
                            CommissioningErrorCode.Unknown,
                            message
                        )
                        finishCommissioningDevice()
                    }
                }
            }

            override fun onPairingComplete(errorCode: Int) {
                super.onPairingComplete(errorCode)
                coroutineScope.launch {
                    if(errorCode != 0){
                        val message = "Pairing completed with code: $errorCode"
                        Timber.e(message)
                        deviceCommissioningCallback?.onError(
                            CommissioningErrorCode.PairingFailed, message
                        )
                        finishCommissioningDevice()
                    }
                }
            }

            override fun onError(error: Throwable?) {
                super.onError(error)
                coroutineScope.launch {
                    val message = "Something went wrong. Cause: ${error?.localizedMessage}"
                    Timber.d(message, error)
                    deviceCommissioningCallback?.onError(CommissioningErrorCode.Unknown, message)
                    finishCommissioningDevice()
                }
            }
        }
    }

    /**
     * Called when device commissioning has completed successfully
     *
     * @param deviceId node identifier
     */
    private fun onCommissioningSuccess(deviceId : Long){
        Timber.d(
        "Commissioned Device: deviceId=$deviceId-$currentDeviceId"
                + " info=[${currentDeviceInfo}]"
        )
        coroutineScope.launch {
            Timber.d("deviceId: $deviceId")
            try {
                Timber.d("Introspecting the device and update the device type")
                val rootNodeEndpoints = clusterManager.fetchRootNodeEndpoints(deviceId)
                Timber.d("**** Matter Device Info ****")
                var deviceType = 0L
                rootNodeEndpoints.forEachIndexed{ index, deviceMatterInfo ->
                    Timber.d("Processing [[${index}] ${deviceMatterInfo}]")
                    if(index != 0){
                        return@forEachIndexed
                    }

                    if (deviceMatterInfo.types.size > 1) {
                        // A proper way might be allowing the user to select a type
                        // Right now we just use the select the first one
                        Timber.w("The device wear many hats")
                    }
                    deviceType = deviceMatterInfo.types.first()
                }

                // get the product identifier if not set
                if(currentDeviceInfo?.productId == 0) {
                    val productId = getProductId(deviceId)
                    currentDeviceInfo?.productId = productId
                }

                // get the vendor identifier if not set
                if (currentDeviceInfo?.vendorId == 0) {
                    val vendorId = getVendorId(deviceId)
                    currentDeviceInfo?.vendorId = vendorId
                }

                // build device info
                val device = MtrDeviceBuilder()
                    .setDeviceId(deviceId)
                    .setDeviceInfo(currentDeviceInfo!!)
                    .setDeviceType(deviceType)
                    .build()
                deviceCommissioningCallback?.onSuccess(device)
                finishCommissioningDevice()
            }catch (ex: Exception){
                deviceCommissioningCallback?.onError(
                    CommissioningErrorCode.TypeIntrospectionFailed,
                    "Failed to introspect device type"
                )
                finishCommissioningDevice()
            }
        }
    }

    /**
     * Getter for the device's product identifier
     *
     * @param deviceId device identifier
     * @return the product identifier
     */
    private suspend fun getProductId(deviceId: Long): Int {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context,deviceId)
        }catch (ex: Exception){
            Timber.e(ex, "Failed to get connected device pointer")
            return 0
        }

        return try {
            clusterManager.readProductIdAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to get device's product Id")
            0
        }
    }

    /**
     * Getter for the device's vendor identifier
     *
     * @param deviceId device identifier
     * @return the vendor identifier
     */
    private suspend fun getVendorId(deviceId: Long): Int {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context,deviceId)
        }catch (ex: Exception){
            Timber.e(ex, "Failed to get connected device pointer")
            return 0
        }

        return try {
            clusterManager.readVendorIdAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to get device's vendor Id")
            0
        }
    }

    /**
     *  Configures the device to start sharing
     *
     *  @param deviceId device identifier
     *  @param discriminator discriminator
     *  @param duration sharing timeout
     */
    suspend fun shareDevice(
        deviceId: Long, discriminator: Int, duration: Int
    ): DeviceSharePayload? = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e( ex, "Failed to get connected device pointer")
            throw DeviceUnreachableException(ex.localizedMessage)
        }

        // Check device share eligibility
        val eligibility = checkDeviceShareEligibility(devicePtr)
        Timber.d("Device share eligibility status: $eligibility")
        if(!eligibility){
            throw UnsupportedOperationException("Device has reached maximum supported commissions")
        }

        // Close any open windows before opening a new one
        try {
            val status = isCommissionWindowOpen(devicePtr)
            Timber.d("Window status: $status")
            if(status) {
                closeCommissionWindow(devicePtr)
            }
        }catch (ex: Exception) {
            Timber.e("Failed to close the window. Cause: ${ex.localizedMessage}")
            throw OperationFailureException("Failed to setup device for sharing")
        }

        // Generate setup pin code
        val setupPinCode = generateSetupPasscode()

        return@withContext suspendCoroutine { continuation ->
            val callback: OpenCommissioningCallback = object : OpenCommissioningCallback {
                override fun onError(status: Int, deviceId: Long) {
                    val errorMsg =
                        "Opening pairing window for device $deviceId failed with status: $status"
                    continuation.resumeWithException(IllegalStateException(errorMsg))
                }

                override fun onSuccess(deviceId: Long, manualCode: String?, qrCode: String?) {
                    if(null == manualCode || null == qrCode){
                        val errorMsg =
                            "Opening pairing window for device $deviceId failed to return payload"
                        continuation.resumeWithException(IllegalStateException(errorMsg))
                    }else{
                        val deviceSharePayload = DeviceSharePayload(
                            qrCode = qrCode,
                            manualCode = manualCode,
                            setupCode = setupPinCode,
                            discriminator = discriminator
                        )
                        continuation.resume(deviceSharePayload)
                    }
                }
            }

            MtrClient.getDeviceController(context).openPairingWindowWithPINCallback(
                devicePtr, duration, ITERATION, discriminator, setupPinCode, callback
            )
        }
    }

    /**
     * Setter for the device name
     *
     * @param deviceId device identifier
     * @param name device name
     * @return true on success, false otherwise
     * @throws DeviceUnreachableException
     * @throws OperationFailureException
     */
    suspend fun renameDevice(
        deviceId: Long, name: String
    ): Boolean = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw DeviceUnreachableException(ex.localizedMessage)
        }

        return@withContext try {
            clusterManager.writeNodeLabelAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID, name)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw OperationFailureException(ex.localizedMessage)
        }
    }

    /**
     * Getter for the device name
     *
     * @param deviceId device identifier
     * @return the device name
     * @throws DeviceUnreachableException
     * @throws OperationFailureException
     */
    suspend fun getDeviceName(deviceId: Long): String  {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw DeviceUnreachableException(ex.localizedMessage)
        }

        return try {
            clusterManager.readNodeLabelAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw OperationFailureException(ex.localizedMessage)
        }
    }

    /**
     * Getter for device unique identifier
     *
     * @param deviceId device identifier
     * @return the device unique identifier
     */
    suspend fun getDeviceUniqueId(deviceId: Long): String = withContext(Dispatchers.Main){
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw DeviceUnreachableException(ex.localizedMessage)
        }

        return@withContext try {
            clusterManager.readUniqueIDAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw OperationFailureException(ex.localizedMessage)
        }
    }





    /**
     * Close commission window if one is open
     *
     * @param devicePtr device pointer
     */
    private fun closeCommissionWindow(devicePtr: Long){
        val timeout = 10000
        val callback = object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
                Timber.d("Window closed")
            }

            override fun onError(ex: Exception) {
                Timber.e("Failed to close window. Cause: ${ex.localizedMessage}")
            }
        }

        ChipClusters.AdministratorCommissioningCluster(devicePtr, ROOT_NODE_ENDPOINT_ID)
            .revokeCommissioning(callback, timeout)
    }

    /**
     * Checks if a device has an open commissioning window
     *
     * @param devicePtr connected device pointer
     * @return the status of the window
     */
    private suspend fun isCommissionWindowOpen(devicePtr: Long): Boolean {
        return suspendCoroutine { continuation ->
            val callback = object : ChipClusters.IntegerAttributeCallback {
                override fun onSuccess(value: Int) {
                    Timber.d("Window status: $value")
                    continuation.resume(value == 1)
                }

                override fun onError(ex: Exception) {
                    Timber.e("Failed to check window status. Cause: ${ex.localizedMessage}")
                    continuation.resumeWithException(ex)
                }
            }

            ChipClusters.AdministratorCommissioningCluster(devicePtr, ROOT_NODE_ENDPOINT_ID)
                .readWindowStatusAttribute(callback)
        }
    }

    /**
     * Checks if the device is eligible for sharing
     *
     * @param devicePtr connected device pointer
     * @return true if eligible, false otherwise.
     */
    private suspend fun checkDeviceShareEligibility(devicePtr: Long): Boolean {
        // Close any open windows before opening a new one
        val supportedFabrics = try {
            clusterManager.readSupportedFabricsAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        }catch (ex: Exception) {
            Timber.e(
                "Failed to read supported fabrics. Cause: ${ex.localizedMessage}"
            )
            throw OperationFailureException("Failed to check device share eligibility")
        }
        Timber.d("Supported fabrics: $supportedFabrics")

        val commissionedFabrics = try {
            clusterManager.readCommissionedFabricsAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        }catch (ex: Exception) {
            Timber.e(
                "Failed to read commissioned. Cause: ${ex.localizedMessage}"
            )
            throw OperationFailureException("Failed to check device share eligibility")
        }

        Timber.d("Commissioned fabrics: $commissionedFabrics")
        return commissionedFabrics < supportedFabrics
    }

    /**
     * Getter for the supported fabrics
     *
     * @param deviceId the device identifier
     * @return the number of supported fabrics
     */
    suspend fun getCommissionedFabrics(deviceId: Long): Int = withContext(Dispatchers.Main) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        } catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw DeviceUnreachableException(ex.localizedMessage)
        } catch (throwable: Throwable) {
            Timber.e(throwable.localizedMessage)
            throw DeviceUnreachableException(throwable.localizedMessage)
        }

        return@withContext try {
            clusterManager.readCommissionedFabricsAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        } catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw OperationFailureException(ex.localizedMessage)
        } catch (throwable: Throwable) {
            Timber.e(throwable.localizedMessage)
            throw OperationFailureException(throwable.localizedMessage)
        }
    }

    /**
     * Getter for the supported fabrics
     *
     * @param deviceId the device identifier
     * @return the number of supported fabrics
     */
    suspend fun getSupportedFabrics(deviceId: Long):Int = withContext(Dispatchers.Main) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        } catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw DeviceUnreachableException(ex.localizedMessage)
        } catch (throwable: Throwable) {
            Timber.e(throwable.localizedMessage)
            throw DeviceUnreachableException(throwable.localizedMessage)
        }

        return@withContext try {
            clusterManager.readSupportedFabricsAttribute(devicePtr, ROOT_NODE_ENDPOINT_ID)
        } catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            throw OperationFailureException(ex.localizedMessage)
        } catch (throwable: Throwable) {
            Timber.e(throwable.localizedMessage)
            throw OperationFailureException(throwable.localizedMessage)
        }
    }

    /**
     * Generates a random setup passcode for device sharing
     */
    private fun generateSetupPasscode(): Long {
        val seed = System.nanoTime()
        return Random(seed).nextLong(100000000)
    }

    companion object {
        /**
         * Share iteration
         */
        private const val ITERATION = 10000L

        /**
         * Set for the fail-safe timer before onDeviceAttestationFailed is invoked.
         *
         * This time depends on the Commissioning timeout of your app.
         */
        private const val ATTESTATION_TIMEOUT = 600

        /**
         * Matter device IANA port address
         */
        private const val DEVICE_IANA_PORT = 5540

        /**
         * Device scan over UDP timeout
         */
        private const val DEVICE_DISCOVERY_TIMEOUT_MS: Long = 7000

        /**
         * Root node endpoint identifier
         */
        private const val ROOT_NODE_ENDPOINT_ID = 0
    }
}
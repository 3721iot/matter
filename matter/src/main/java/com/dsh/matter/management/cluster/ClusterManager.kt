package com.dsh.matter.management.cluster

import android.content.Context
import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipClusters.*
import chip.devicecontroller.ChipStructs
import chip.devicecontroller.ReportCallback
import chip.devicecontroller.model.ChipAttributePath
import chip.devicecontroller.model.ChipEventPath
import chip.devicecontroller.model.NodeState
import chip.tlv.AnonymousTag
import chip.tlv.TlvReader
import com.dsh.matter.MtrClient
import com.dsh.matter.management.device.DeviceManager
import com.dsh.matter.management.device.OperationFailureException
import com.dsh.matter.model.cluster.EndpointInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
internal class ClusterManager @Inject constructor(private val context: Context) {

    /**
     * Fetches MatterDeviceInfo for each endpoint supported by the device.
     *
     * @param deviceId node identifier
     */
    suspend fun fetchRootNodeEndpoints(deviceId: Long): List<EndpointInfo> {
        val nodeClusters = arrayListOf<EndpointInfo>()
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        } catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            return emptyList()
        }

        val partsListAttr = readDescriptorClusterPartsListsAttr(devicePtr, ROOT_NODE_ENDPOINT_ID.toInt())
        partsListAttr?.forEach { part ->
            Timber.d("part [$part] is [${part.javaClass}]")
            val endpoint = when( part ) {
                is Int -> part.toInt()
                else -> return@forEach
            }

            // DeviceListAttribute
            val deviceListAttr = readDescriptorClusterDeviceListAttr(devicePtr, endpoint)
            val types = arrayListOf<Long>()
            deviceListAttr.forEach { types.add(it.deviceType) }

            //Build EndpointInfo
            val endpointInfo = EndpointInfo(endpoint, types)
            nodeClusters.add(endpointInfo)
        }
        return nodeClusters
    }

    /**
     * DeviceTypeListAttributeCallback
     *
     * @param devicePtr connected device pointer
     * @param endpointId device's endpoint
     */
    private suspend fun readDescriptorClusterDeviceListAttr(
        devicePtr: Long,
        endpointId: Int
    ): List<ChipStructs.DescriptorClusterDeviceTypeStruct> {
        return suspendCoroutine { continuation ->
            DescriptorCluster(devicePtr, endpointId)
                .readDeviceTypeListAttribute(
                    object : DescriptorCluster.DeviceTypeListAttributeCallback {
                        override fun onSuccess(
                            values: List<ChipStructs.DescriptorClusterDeviceTypeStruct>
                        ) {
                            continuation.resume(values)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    }
                )
        }
    }

    /**
     * DeviceListAttribute
     *
     * ```
     * For example, on endpoint 0:
     *   device: [long type: 22, int revision: 1] -> maps to Root node (0x0016) (utility device type)
     * on endpoint 1:
     *   device: [long type: 256, int revision: 1] -> maps to On/Off Light (0x0100)
     * ```
     * @param devicePtr connected device pointer
     * @param endpointId device's endpoint
     */
    private suspend fun readDescriptorClusterPartsListsAttr(
        devicePtr: Long,
        endpointId: Int
    ): List<Any> ? {
        return suspendCoroutine {  continuation ->
            DescriptorCluster(devicePtr, endpointId)
                .readPartsListAttribute(
                    object : DescriptorCluster.PartsListAttributeCallback {
                        override fun onSuccess(values: MutableList<Int>?) {
                            continuation.resume(values)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    }
                )
        }
    }

    /**
     * Reads the current fabric identifier for a device
     *
     * @param devicePtr connected device pointer
     */
    suspend fun readCurrentFabricIndex(devicePtr: Long): UInt {
        return suspendCoroutine { continuation ->
            val callback = object : ReportCallback {
                override fun onError(
                    attributePath: ChipAttributePath?,
                    eventPath: ChipEventPath?,
                    ex: Exception
                ) {
                    val errMsg = "Read current fabric index failed. Cause: ${ex.localizedMessage}"
                    Timber.e(errMsg)
                    continuation.resumeWithException(OperationFailureException(errMsg))
                }

                override fun onReport(nodeState: NodeState?) {
                    val state =  nodeState?.getEndpointState(ROOT_NODE_ENDPOINT_ID.toInt())
                        ?.getClusterState(OperationalCredentialsCluster.CLUSTER_ID)
                        ?.getAttributeState(CURRENT_FABRIC_INDEX_ATTR_ID)
                    if(null == state) {
                        val errMsg = "Node state reported null"
                        continuation.resumeWithException(OperationFailureException(errMsg))
                    }else{
                        continuation.resume(
                            TlvReader(state.tlv).getUInt(AnonymousTag)
                        )
                    }
                }
            }

            MtrClient.getDeviceController(context).readAttributePath(
                callback,
                devicePtr,
                listOf(
                    ChipAttributePath.newInstance(
                        ROOT_NODE_ENDPOINT_ID,
                        OperationalCredentialsCluster.CLUSTER_ID,
                        CURRENT_FABRIC_INDEX_ATTR_ID
                    )
                ),
                0
            )
        }
    }

    /**
     * Reads the device/node label
     *
     * @param devicePtr device pointer
     * @param endpointId endpoint identifier
     *
     * @return the node label
     */
    suspend fun readNodeLabelAttribute(devicePtr: Long, endpointId: Int): String {
        return suspendCoroutine { continuation ->
            val callback = object : CharStringAttributeCallback {
                override fun onSuccess(value: String) {
                    continuation.resume(value)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex,"Failed to read NodeLabel attribute")
                    continuation.resumeWithException(ex)
                }
            }

            BasicInformationCluster(devicePtr, endpointId).readNodeLabelAttribute(callback)
        }
    }

    /**
     * Writes the node/device label
     *
     * @param devicePtr connected device pointer
     * @param endpointId endpoint identifier
     * @param nodeLabel node label
     *
     * @return the operation status
     */
    suspend fun writeNodeLabelAttribute(devicePtr: Long, endpointId: Int, nodeLabel: String): Boolean {
        return suspendCoroutine { continuation ->
            val callback = object : DefaultClusterCallback {
                override fun onSuccess() {
                    continuation.resume(true)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to write NodeLabel attribute")
                    continuation.resumeWithException(ex)
                }
            }

            BasicInformationCluster(devicePtr, endpointId).writeNodeLabelAttribute(
                callback, nodeLabel
            )
        }
    }

    /**
     * Reads product name
     *
     * @param devicePtr connected device pointer
     * @param endpointId endpoint identifier
     * @return the product name
     */
    suspend fun readProductNameAttribute(devicePtr: Long, endpointId: Int): String {
        return suspendCoroutine { continuation ->
            val callback = object : CharStringAttributeCallback {
                override fun onSuccess(value: String) {
                    continuation.resume(value)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to read ProductName attribute")
                    continuation.resumeWithException(ex)
                }
            }

            BasicInformationCluster(devicePtr, endpointId).readProductNameAttribute(callback)
        }
    }

    /**
     * Read product identifier
     *
     * @param devicePtr the connected device pointer
     * @param endpointId the endpoint identifier
     * @return the product identifier
     */
    suspend fun readProductIdAttribute(devicePtr: Long, endpointId: Int): Int {
        return suspendCoroutine { continuation ->
            val callback = object : IntegerAttributeCallback {
                override fun onSuccess(value: Int) {
                    continuation.resume(value)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to read ProductId attribute")
                    continuation.resumeWithException(ex)
                }
            }

            BasicInformationCluster(devicePtr, endpointId).readProductIDAttribute(callback)
        }
    }

    /**
     * Reads the vendor name attribute
     *
     * @param devicePtr connected device pointer
     * @param endpointId endpoint identifier
     *
     * @return the vendor name
     */
    suspend fun readVendorNameAttribute(devicePtr: Long, endpointId: Int): String {
        return suspendCoroutine { continuation ->
            val callback = object : CharStringAttributeCallback {
                override fun onSuccess(value: String) {
                    continuation.resume(value)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to read VendorName attribute")
                    continuation.resumeWithException(ex)
                }
            }

            BasicInformationCluster(devicePtr, endpointId).readVendorNameAttribute(callback)
        }
    }

    /**
     * Reads the vendor identifier attribute
     *
     * @param devicePtr device pointer
     * @param endpointId endpoint identifier
     *
     * @return the vendor identifier
     */
    suspend fun readVendorIdAttribute(devicePtr: Long, endpointId: Int): Int {
        return suspendCoroutine { continuation ->
            val callback = object : IntegerAttributeCallback {
                override fun onSuccess(value: Int) {
                    continuation.resume(value)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to read VendorId attribute")
                    continuation.resumeWithException(ex)
                }
            }

            BasicInformationCluster(devicePtr, endpointId).readVendorIDAttribute(callback)
        }
    }

    /**
     * Reads unique identifier attribute
     *
     * @param devicePtr connected device pointer
     * @param endpointId endpoint identifier
     *
     * @return the unique identifier
     */
    suspend fun readUniqueIDAttribute(devicePtr: Long, endpointId: Int): String {
        return suspendCoroutine { continuation ->
            val callback = object : CharStringAttributeCallback {
                override fun onSuccess(value: String) {
                    continuation.resume(value)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to read Unique ID attribute")
                    continuation.resumeWithException(ex)
                }
            }

            BasicInformationCluster(devicePtr, endpointId).readUniqueIDAttribute(callback)
        }
    }

    /**
     * Reads the number of Fabrics that are supported by the device. Note: this value is fixed
     * for a particular device. (See spec section "11.17.6.3. SupportedFabrics Attribute")
     *
     * @param devicePtr connected deice pointer.
     * @param endpointId endpoint identifier.
     */
    suspend fun readSupportedFabricsAttribute(devicePtr: Long, endpointId: Int): Int {
        return suspendCoroutine { continuation ->
            val callback = object : IntegerAttributeCallback{
                override fun onSuccess(value: Int) {
                    continuation.resume(value);
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to read supported fabrics")
                    continuation.resumeWithException(ex)
                }
            }
            OperationalCredentialsCluster(devicePtr, endpointId)
                .readSupportedFabricsAttribute(callback)
        }
    }

    /**
     * Reads the number of Fabrics to which the device is currently commissioned.
     * (See spec section "11.17.6.4. CommissionedFabrics Attribute")
     * @param devicePtr connected device pointer.
     * @param endpointId endpoint identifier.
     *
     * @return the number of Fabrics to which the device is currently commissioned
     */
    suspend fun readCommissionedFabricsAttribute(devicePtr: Long, endpointId: Int): Int {
        return suspendCoroutine { continuation ->
            val callback = object : IntegerAttributeCallback{
                override fun onSuccess(value: Int) {
                    continuation.resume(value);
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to read commissioned fabrics")
                    continuation.resumeWithException(ex)
                }
            }
            OperationalCredentialsCluster(devicePtr, endpointId)
                .readCommissionedFabricsAttribute(callback)
        }
    }

    /**
     * Fetches the device's on off cluster
     * @param devicePtr connected device pointer
     * @param endpointId OnOff cluster endpoint
     */
    suspend fun readOnOffAttribute(devicePtr: Long, endpointId: Int): Boolean{
        return withTimeoutOrNull(READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : BooleanAttributeCallback {
                    override fun onSuccess(status: Boolean) {
                        continuation.resume(status)
                    }

                    override fun onError(ex: Exception) {
                        Timber.e(ex,"Failed to read OnOffCluster attribute")
                        continuation.resumeWithException(ex)
                    }
                }
                OnOffCluster(devicePtr, endpointId).readOnOffAttribute(callback)
            }
        }?: throw ReadClusterAttributeException("Read OnOffCluster attribute failed")
    }

    /**
     * Fetches the device's current level from [ChipClusters.LevelControlCluster]
     *
     * @param devicePtr connected device pointer
     * @param endpointId OnOff cluster endpoint
     */
    suspend fun readCurrentLevelAttribute(devicePtr: Long, endpointId: Int): Int {
        return withTimeoutOrNull(READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : LevelControlCluster.CurrentLevelAttributeCallback {
                    override fun onSuccess(value: Int?) {
                        continuation.resume(value)
                    }

                    override fun onError(ex: Exception) {
                        continuation.resumeWithException(ex)
                    }
                }
                LevelControlCluster(devicePtr, endpointId).readCurrentLevelAttribute(callback)
            }
        } ?: throw ReadClusterAttributeException("Read LevelControlCluster attribute failed")
    }

    /**
     * Fetches the device's current color temperature from [ChipClusters.ColorControlCluster]
     *
     * @param devicePtr connected device pointer
     * @param endpointId OnOff cluster endpoint
     */
    suspend fun readColorTemperatureAttribute(devicePtr: Long, endpointId: Int): Int {
        return withTimeoutOrNull(READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : IntegerAttributeCallback{
                    override fun onSuccess(value: Int) {
                        continuation.resume(value)
                    }

                    override fun onError(ex: Exception) {
                        continuation.resumeWithException(ex)
                    }
                }
                ColorControlCluster(devicePtr, endpointId)
                    .readColorTemperatureMiredsAttribute(callback)
            }
        }?: throw ReadClusterAttributeException("Read ColorControlCluster mired attribute failed")
    }

    /**
     * Fetches device's current hue value from [ChipClusters.ColorControlCluster]
     *
     * @param devicePtr device pointer
     * @param endpointId node endpoint
     */
    suspend fun readCurrentHueAttribute(devicePtr: Long, endpointId: Int): Int {
        return withTimeoutOrNull(READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : IntegerAttributeCallback {
                    override fun onSuccess(value: Int) {
                        continuation.resume(value)
                    }

                    override fun onError(ex: Exception) {
                        Timber.e("failed to read current hue attribute")
                        continuation.resumeWithException(ex)
                    }
                }
                ColorControlCluster(devicePtr, endpointId).readCurrentHueAttribute(callback)
            }
        }?: throw ReadClusterAttributeException("Read ColorControlCluster hue attribute failed")
    }

    /**
     * Fetches device's current saturation value from [ChipClusters.ColorControlCluster]
     *
     * @param devicePtr device pointer
     * @param endpointId node endpoint
     */
    suspend fun readCurrentSaturationAttribute(devicePtr: Long, endpointId: Int): Int {
        return withTimeoutOrNull(READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : IntegerAttributeCallback {
                    override fun onSuccess(value: Int) {
                        Timber.d("received saturation: $value")
                        continuation.resume(value)
                    }

                    override fun onError(ex: Exception) {
                        Timber.e("failed to read current saturation attribute")
                        continuation.resumeWithException(ex)
                    }
                }

                ColorControlCluster(devicePtr, endpointId).readCurrentSaturationAttribute(callback)
            }
        }?: throw ReadClusterAttributeException("Read ColorControlCluster saturation attribute failed")
    }

    /**
     * Fetches device's current fan mode value from [ChipClusters.FanControlCluster]
     *
     * @param devicePtr device pointer
     * @param endpointId node endpoint
     */
    suspend fun readFanMode(devicePtr: Long, endpointId: Int): Int {
        return withTimeoutOrNull(READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : IntegerAttributeCallback {
                    override fun onSuccess(value: Int) {
                        Timber.d("received fan mode: $value")
                        continuation.resume(value)
                    }

                    override fun onError(ex: Exception) {
                        Timber.e("failed to read current fan mode attribute")
                        continuation.resumeWithException(ex)
                    }
                }

                FanControlCluster(devicePtr, endpointId).readFanModeAttribute(callback)
            }
        }?: throw ReadClusterAttributeException("Read FanControlCluster mode attribute failed")
    }

    /**
     * Fetches device's current fan percentage setting value from [ChipClusters.FanControlCluster]
     *
     * @param devicePtr device pointer
     * @param endpointId node endpoint
     */
    suspend fun readFanPercentageSetting(devicePtr: Long, endpointId: Int): Int {
        return withTimeoutOrNull(READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : FanControlCluster.PercentSettingAttributeCallback {
                    override fun onSuccess(value: Int?) {
                        Timber.d("received fan percentage value: $value")
                        continuation.resume(value)
                    }

                    override fun onError(ex: Exception) {
                        Timber.e("failed to read current percentage value attribute")
                        continuation.resumeWithException(ex)
                    }
                }

                FanControlCluster(devicePtr, endpointId).readPercentSettingAttribute(callback)
            }
        }?: throw ReadClusterAttributeException("Read FanControlCluster percentage attribute failed")
    }

    /**
     * Subscribe to OnOff cluster attribute
     *
     * @param devicePtr connected device pointer
     * @param endpointId endpoint identifier
     * @param callback subscription callback
     */
    fun subscribeOnOffAttribute(
        devicePtr: Long, endpointId: Int, callback: BooleanAttributeCallback
    ) {
        OnOffCluster(devicePtr, endpointId)
            .subscribeOnOffAttribute(callback, MIN_SUB_INTERVAL_MS, MAX_SUB_INTERVAL_MS)
    }

    /**
     * Subscribe to LevelControl cluster attribute
     *
     * @param devicePtr connected device pointer
     * @param endpointId endpoint identifier
     * @param callback subscription callback
     */
    fun subscribeCurrentLevelAttribute(
        devicePtr: Long, endpointId: Int,callback: LevelControlCluster.CurrentLevelAttributeCallback
    ) {
        LevelControlCluster(devicePtr, endpointId)
            .subscribeCurrentLevelAttribute(callback, MIN_SUB_INTERVAL_MS, MAX_SUB_INTERVAL_MS)
    }

    /**
     * Subscribe to ColorControl cluster attribute
     *
     * @param devicePtr connected device pointer
     * @param endpointId endpoint identifier
     * @param callback subscription callback
     */
    fun subscribeColorAttribute(
        devicePtr: Long, endpointId: Int,callback: IntegerAttributeCallback
    ) {
        ColorControlCluster(devicePtr, endpointId)
            .subscribeCurrentHueAttribute(callback, MIN_SUB_INTERVAL_MS, MAX_SUB_INTERVAL_MS)
    }

    companion object {
        /**
         *
         */
        private const val MIN_SUB_INTERVAL_MS = 10

        /**
         *
         */
        private const val MAX_SUB_INTERVAL_MS = 60

        /**
         * Root node endpoint identifier
         */
        private const val ROOT_NODE_ENDPOINT_ID = 0L

        /**
         * Current fabric index attribute identifier
         */
        private const val CURRENT_FABRIC_INDEX_ATTR_ID = 5L

        /**
         * Timeout for reading data from cluster attributes.
         */
        private const val READ_CLUSTER_ATTRIBUTE_TIMEOUT_MS = 1000L
    }
}
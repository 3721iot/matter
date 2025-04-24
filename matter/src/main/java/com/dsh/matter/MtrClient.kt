package com.dsh.matter

import android.content.Context
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.ControllerParams
import chip.devicecontroller.GetConnectedDeviceCallbackJni
import chip.platform.*
import com.dsh.matter.management.device.DeviceUnreachableException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal object MtrClient {

    /**
     * Initialize with a test vendor ID
     * @note should be replaced with a company assigned VID
     */
    private const val VENDOR_ID = 0xFFF4

    /**
     * Android platform
     */
    private lateinit var androidPlatform: AndroidChipPlatform

    /**
     * Device controller
     */
    private lateinit var chipDeviceController: ChipDeviceController

    /**
     *  Lazily Instantiate [ChipDeviceController] and holds a reference to it.
     *
     *  @return an instance of the CHIP device controller
     */
    fun getDeviceController(context: Context): ChipDeviceController {
        getAndroidPlatform(context)
        if(!this::chipDeviceController.isInitialized) {
            chipDeviceController = ChipDeviceController(
                ControllerParams
                    .newBuilder()
                    .setUdpListenPort(0)
                    .setControllerVendorId(VENDOR_ID)
                    .build()
            )
            // ToDo() Set delegate for attestation trust store for device attestation verifier
        }
        return chipDeviceController
    }

    /**
     *  Lazily Instantiate [AndroidChipPlatform] and holds a reference to it.
     *
     *  @return an instance of the Android CHIP platform
     */
    fun getAndroidPlatform(context: Context) : AndroidChipPlatform {
        if(!this::androidPlatform.isInitialized){
            ChipDeviceController.loadJni()
            androidPlatform = AndroidChipPlatform(
                AndroidBleManager(),
                PreferencesKeyValueStoreManager(context),
                PreferencesConfigurationManager(context),
                NsdManagerServiceResolver(context),
                NsdManagerServiceBrowser(context),
                ChipMdnsCallbackImpl(),
                DiagnosticDataProviderImpl(context))
        }

        return androidPlatform
    }

    /**
     *  Wraps [ChipDeviceController.getConnectedDevicePointer] to return the value directly.
     *
     *  @note (#21539) This is a memory leak because we currently never call
     *  releaseConnectedDevicePointer once we are done with the returned device pointer.
     *  Memory leak was introduced since the refactor that introduced it was very large
     *  in order to fix a use after free, which was considered to be worse than the memory
     *  leak that was introduced.
     *
     * @param deviceId node identifier
     * @return the connected device pointer
     */
    suspend fun getConnectedDevicePointer(context: Context, deviceId: Long): Long {
        return suspendCoroutine { continuation ->
            val callback = object : GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                override fun onDeviceConnected(devicePointer: Long) {
                    continuation.resume(devicePointer)
                }

                override fun onConnectionFailure(nodeId: Long, exc: Exception?) {
                    val errorMsg = "Failed to reach device $deviceId"
                    Timber.e(errorMsg)
                    continuation.resumeWithException(DeviceUnreachableException(errorMsg))
                }
            }
            getDeviceController(context).getConnectedDevicePointer(deviceId, callback)
        }
    }

    /**
     *  Wraps [ChipDeviceController.getConnectedDevicePointer] to return the value directly.
     *
     *  @note (#21539) This is a memory leak because we currently never call
     *  releaseConnectedDevicePointer once we are done with the returned device pointer.
     *  Memory leak was introduced since the refactor that introduced it was very large
     *  in order to fix a use after free, which was considered to be worse than the memory
     *  leak that was introduced.
     *
     * @param context Android app context
     * @param deviceId node identifier
     * @param timeoutMillis timeout
     * @return the connected device pointer
     */
    suspend fun getConnectedDevicePointer(context: Context, deviceId: Long, timeoutMillis: Long): Long {
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                    override fun onDeviceConnected(devicePointer: Long) {
                        continuation.resume(devicePointer)
                    }

                    override fun onConnectionFailure(nodeId: Long, exc: Exception?) {
                        val errorMsg = "Failed to reach device $deviceId"
                        Timber.e(errorMsg)
                    }
                }
                getDeviceController(context).getConnectedDevicePointer(deviceId, callback)
            }
        } ?: throw DeviceUnreachableException("Failed to reach device $deviceId")
    }
}
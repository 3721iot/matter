package com.dsh.matter.management.device

import android.content.Context
import chip.devicecontroller.ChipClusters
import com.dsh.matter.MtrClient
import com.dsh.matter.model.color.HSVColor
import com.dsh.matter.model.device.FanMode
import com.dsh.matter.util.AttributeUtils
import com.dsh.matter.util.AttributeUtils.MTR_MAX_BRIGHTNESS
import com.dsh.matter.util.AttributeUtils.MTR_MAX_COLOR_TEMPERATURE
import com.dsh.matter.util.AttributeUtils.MTR_MAX_HUE
import com.dsh.matter.util.AttributeUtils.MTR_MAX_SATURATION
import com.dsh.matter.util.AttributeUtils.MTR_MIN_COLOR_TEMPERATURE
import com.dsh.matter.util.AttributeUtils.STD_MAX_BRIGHTNESS
import com.dsh.matter.util.AttributeUtils.STD_MAX_COLOR_TEMPERATURE
import com.dsh.matter.util.AttributeUtils.STD_MAX_HUE
import com.dsh.matter.util.AttributeUtils.STD_MAX_SATURATION
import com.dsh.matter.util.AttributeUtils.STD_MIN_COLOR_TEMPERATURE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class DeviceController @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Sets the value for device switch cluster
     * @param deviceId device identifier
     * @param on value
     */
    @Throws(OperationFailureException::class)
    suspend fun power(deviceId: Long, on: Boolean): Unit = withContext(Dispatchers.IO){
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            return@withContext
        }

        return@withContext suspendCoroutine { continuation ->
            val callback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.d("Command [power=${on}] sent successfully")
                    continuation.resume(Unit)
                }

                override fun onError(ex: Exception) {
                    Timber.e(ex, "Failed to send command [power=${on}]")
                    continuation.resumeWithException(OperationFailureException(cause = ex))
                }
            }

            when (on) {
                true -> {
                    ChipClusters.OnOffCluster(devicePtr, NODE_CTRL_ENDPOINT_ID).on(callback)
                }
                else -> {
                    ChipClusters.OnOffCluster(devicePtr, NODE_CTRL_ENDPOINT_ID).off(callback)
                }
            }
        }
    }

    /**
     * Sets the value for device switch cluster
     * @param deviceId device identifier
     * @param on value
     * @param callback: DeviceControllerCallback
     */
    suspend fun power(
        deviceId: Long,
        on: Boolean,
        callback: DeviceControllerCallback
    ): Unit = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            callback.onError(ex)
            return@withContext
        }

        val defaultCallback = object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onError(ex: Exception?) {
                callback.onError(OperationFailureException(cause = ex))
            }
        }

        return@withContext when(on){
            true -> {
                ChipClusters.OnOffCluster(devicePtr, NODE_CTRL_ENDPOINT_ID).on(
                    defaultCallback
                )
            }
            else ->{
                ChipClusters.OnOffCluster(devicePtr, NODE_CTRL_ENDPOINT_ID).off(
                    defaultCallback
                )
            }
        }
    }

    /**
     * Sets the value for device LevelControl cluster
     *
     * @param deviceId device identifier
     * @param brightness value
     */
    @Throws(OperationFailureException::class)
    suspend fun brightness(deviceId: Long, brightness: Int): Unit = withContext(Dispatchers.IO){
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            return@withContext
        }

        return@withContext suspendCoroutine { continuation ->
            val callback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.d("Command [brightness=${brightness}] sent successfully")
                    continuation.resume(Unit)
                }

                override fun onError(ex: Exception) {
                    val errMsg = "Failed to send command [brightness=${brightness}]"
                    Timber.d(errMsg)
                    continuation.resumeWithException(
                        OperationFailureException(message = errMsg, cause = ex)
                    )
                }
            }

            ChipClusters.LevelControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID).moveToLevel(
                callback,
                AttributeUtils.mapValue(brightness, STD_MAX_BRIGHTNESS, MTR_MAX_BRIGHTNESS),
                0,
                0,
                0
            )
        }
    }

    /**
     * Sets the value for device LevelControl cluster
     *
     * @param deviceId device identifier
     * @param brightness value
     * @param callback DeviceControllerCallback
     */
    suspend fun brightness(
        deviceId: Long, brightness: Int, callback: DeviceControllerCallback
    ): Unit = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            callback.onError(ex)
            return@withContext
        }

        val defaultCallback = object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onError(ex: Exception?) {
                callback.onError(OperationFailureException(cause = ex))
            }

        }

        return@withContext ChipClusters
            .LevelControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
            .moveToLevel(
                defaultCallback,
                AttributeUtils.mapValue(brightness, STD_MAX_BRIGHTNESS, MTR_MAX_BRIGHTNESS),
                0, 0, 0
            )
    }

    /**
     * Sets the value for the Hue and Saturation feature in the ColorControl cluster
     * @note the value in HSV is not sent to the device.
     * To control device's brightness, please refer to [brightness] methods
     *
     * @param deviceId device identifier
     * @param color color in HSV color space. hue=[0-360], saturation=[0-100], value=[0-100]
     */
    @Throws(OperationFailureException::class)
    suspend fun color(deviceId: Long, color: HSVColor): Unit = withContext(Dispatchers.IO){
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            return@withContext
        }

        return@withContext suspendCoroutine { continuation ->
            val callback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.d("Command [color=${color}] sent successfully")
                    continuation.resume(Unit)
                }

                override fun onError(ex: Exception) {
                    val errorMsg = "Failed to send command [color=${color}]"
                    Timber.d(errorMsg)
                    continuation.resumeWithException(OperationFailureException(cause = ex))
                }
            }

            ChipClusters.ColorControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
                .moveToHueAndSaturation(
                    callback,
                    AttributeUtils.mapValue(color.hue, STD_MAX_HUE, MTR_MAX_HUE),
                    AttributeUtils
                        .mapValue(color.saturation, STD_MAX_SATURATION, MTR_MAX_SATURATION),
                    0,
                    0,
                    0
                )
        }
    }

    /**
     * Sets the value for the Hue and Saturation feature in the ColorControl cluster
     * @param deviceId device identifier
     * @param color color in HSV color space. hue=[0-360], saturation=[0-100], value=[0-100]
     * @param callback [DeviceControllerCallback]
     */
    suspend fun color(
        deviceId: Long, color: HSVColor, callback: DeviceControllerCallback
    ): Unit = withContext(Dispatchers.IO){
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            callback.onError(ex)
            return@withContext
        }

        val defaultCallback = object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onError(ex: Exception?) {
                callback.onError(OperationFailureException(cause = ex))
            }
        }

        ChipClusters.ColorControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
            .moveToHueAndSaturation(
                defaultCallback,
                AttributeUtils.mapValue(color.hue, STD_MAX_HUE, MTR_MAX_HUE),
                AttributeUtils.mapValue(color.saturation, STD_MAX_SATURATION, MTR_MAX_SATURATION),
                0,
                0,
                0
            )
    }

    /**
     * Sets the value for device LevelControl cluster
     *
     * @param deviceId device identifier
     * @param temperature color temperature
     */
    @Throws(OperationFailureException::class)
    suspend fun colorTemperature(
        deviceId: Long, temperature: Int
    ): Unit = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            return@withContext
        }

        return@withContext suspendCoroutine { continuation ->
            val callback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.d("Command [temperature=${temperature}] sent successfully")
                    continuation.resume(Unit)
                }

                override fun onError(ex: Exception) {
                    val errorMsg = "Failed to send command [temperature=${temperature}]"
                    Timber.d(errorMsg)
                    continuation.resumeWithException(
                        OperationFailureException(message = errorMsg, cause = ex)
                    )
                }
            }

            ChipClusters.ColorControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
                .moveToColorTemperature(
                    callback,
                    AttributeUtils.mapValue(
                        temperature,
                        STD_MIN_COLOR_TEMPERATURE,
                        STD_MAX_COLOR_TEMPERATURE,
                        MTR_MIN_COLOR_TEMPERATURE,
                        MTR_MAX_COLOR_TEMPERATURE
                    ),
                    0, 0, 0
                )
        }
    }

    /**
     * Sets the color temperature value for device Color Control cluster
     *
     * @param deviceId device identifier
     * @param temperature color temperature
     * @param callback DeviceControllerCallback
     */
    suspend fun colorTemperature(
        deviceId: Long, temperature : Int, callback: DeviceControllerCallback
    ): Unit = withContext(Dispatchers.IO){
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            callback.onError(ex)
            return@withContext
        }

        return@withContext suspendCoroutine {
            val defaultCallback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    callback.onSuccess()
                }

                override fun onError(ex: Exception?) {
                    callback.onError(OperationFailureException(cause = ex))
                }

            }

            ChipClusters.ColorControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
                .moveToColorTemperature(
                    defaultCallback,
                    AttributeUtils.mapValue(
                        temperature,
                        STD_MIN_COLOR_TEMPERATURE,
                        STD_MAX_COLOR_TEMPERATURE,
                        MTR_MIN_COLOR_TEMPERATURE,
                        MTR_MAX_COLOR_TEMPERATURE,
                    ),
                    0, 0, 0
            )
        }
    }

    /**
     * Sets the fan mode value for [ChipClusters.FanControlCluster]
     *
     * @param deviceId device identifier
     * @param fanMode fan mode
     */
    suspend fun fanMode(deviceId: Long, fanMode: FanMode): Unit = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            return@withContext
        }

        return@withContext suspendCoroutine { continuation ->
            val callback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.d("Command [mode=${fanMode}] sent successfully")
                    continuation.resume(Unit)
                }

                override fun onError(ex: Exception) {
                    val errorMsg = "Failed to send command [mode=${fanMode}]"
                    Timber.d(errorMsg)
                    continuation.resumeWithException(
                        OperationFailureException(message = errorMsg, cause = ex)
                    )
                }
            }

            ChipClusters.FanControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
                .writeFanModeAttribute(callback, fanMode.mode)
        }
    }

    /**
     * Sets the fan mode value for [ChipClusters.FanControlCluster]
     *
     * @param deviceId device identifier
     * @param fanMode fan mode
     * @param callback [DeviceControllerCallback]
     */
    suspend fun fanMode(
        deviceId: Long, fanMode: FanMode, callback: DeviceControllerCallback
    ): Unit = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            callback.onError(ex)
            return@withContext
        }

        return@withContext suspendCoroutine {
            val defaultCallback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    callback.onSuccess()
                }

                override fun onError(ex: Exception?) {
                    callback.onError(OperationFailureException(cause = ex))
                }
            }

            ChipClusters.FanControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
                .writeFanModeAttribute(defaultCallback, fanMode.mode)
        }
    }


    /**
     * Sets the fan mode value for [ChipClusters.FanControlCluster]
     *
     * @param deviceId device identifier
     * @param speed fan speed
     */
    suspend fun fanSpeed(deviceId: Long, speed: Int) : Unit = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            return@withContext
        }

        return@withContext suspendCoroutine { continuation ->
            val callback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.d("Command [speed=${speed}] sent successfully")
                    continuation.resume(Unit)
                }

                override fun onError(ex: Exception) {
                    val errorMsg = "Failed to send command [speed=${speed}]"
                    Timber.d(errorMsg)
                    continuation.resumeWithException(
                        OperationFailureException(message = errorMsg, cause = ex)
                    )
                }
            }

            // ToDo(): check if setting percentage setting only instead of speed will cause any
            //  issues
            ChipClusters.FanControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
                .writePercentSettingAttribute(callback, speed)
        }
    }

    /**
     * Sets the fan speed value for [ChipClusters.FanControlCluster]
     *
     * @param deviceId device identifier
     * @param speed fan speed [0 - 100]
     * @param callback [DeviceControllerCallback]
     */
    suspend fun fanSpeed(
        deviceId: Long, speed: Int, callback: DeviceControllerCallback
    ): Unit = withContext(Dispatchers.IO) {
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            callback.onError(ex)
            return@withContext
        }

        return@withContext suspendCoroutine {
            val defaultCallback = object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    callback.onSuccess()
                }

                override fun onError(ex: Exception?) {
                    callback.onError(OperationFailureException(cause = ex))
                }
            }

            // ToDo(): check if setting percentage setting only instead of speed will cause any
            //  issues
            ChipClusters.FanControlCluster(devicePtr, NODE_CTRL_ENDPOINT_ID)
                .writePercentSettingAttribute(defaultCallback, speed)
        }
    }

    companion object {
        /**
         * Matter device ctrl default endpoint identifier
         */
        private const val NODE_CTRL_ENDPOINT_ID = 1
    }
}
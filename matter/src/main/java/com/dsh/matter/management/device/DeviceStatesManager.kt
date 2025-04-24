package com.dsh.matter.management.device

import android.content.Context
import chip.devicecontroller.ReportCallback
import chip.devicecontroller.ResubscriptionAttemptCallback
import chip.devicecontroller.SubscriptionEstablishedCallback
import chip.devicecontroller.model.ChipAttributePath
import chip.devicecontroller.model.ChipEventPath
import chip.devicecontroller.model.ChipPathId
import chip.devicecontroller.model.NodeState
import com.dsh.matter.MtrClient
import com.dsh.matter.management.cluster.ClusterManager
import com.dsh.matter.management.cluster.ReadClusterAttributeException
import com.dsh.matter.model.color.HSVColor
import com.dsh.matter.model.device.DeviceStateChangeTask
import com.dsh.matter.model.device.DeviceType
import com.dsh.matter.model.device.FanMode
import com.dsh.matter.model.device.StateAttribute
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
import com.dsh.matter.util.ClusterUtils
import com.dsh.matter.util.device.DeviceTypeUtil
import com.dsh.matter.util.device.FanModeUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.apache.commons.math3.special.Erf
import org.jetbrains.annotations.ApiStatus.Experimental
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine
import kotlin.math.round
import kotlin.math.sqrt

@Singleton
class DeviceStatesManager @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Inject dependency repos
     */
    private val clusterManager: ClusterManager = ClusterManager(context)

    /**
     * Device state change job
     */
    private var stateChangeJob : Job? = Job()

    /**
     * Device state change job's scope.
     * ToDo() check if this will hold when there are more devices
     */
    private val stateChangeJobScope = CoroutineScope(Dispatchers.Default)

    /**
     * Device state change tasks
     */
    private val stateChangeTasks: HashMap<Long, DeviceStateChangeTask> = hashMapOf()


    /**
     * Checks if a device is online or not
     *
     * @param deviceId device identifier
     * @param timeoutMillis timeout in milliseconds.
     */
    @Experimental
    suspend fun isDeviceOnline(
        deviceId: Long, timeoutMillis: Long? = DEVICE_CON_TIMEOUT_MS
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            MtrClient.getConnectedDevicePointer(
                context,
                deviceId,
                timeoutMillis ?: DEVICE_CON_TIMEOUT_MS)
        } catch (ex: Exception) {
            return@withContext false
        } catch (thr: Throwable) {
            return@withContext false
        }

        return@withContext true
    }

    /**
     * Fetches device's current states
     *
     * @param deviceId device identifier
     */
    @Deprecated(
        message = "Use readDeviceStates(deviceId, deviceType)",
        replaceWith = ReplaceWith("readDeviceStates(deviceId, deviceType)"),
        level = DeprecationLevel.ERROR
    )
    suspend fun readDeviceStates(
        deviceId: Long
    ): HashMap<StateAttribute, Any> = withContext(Dispatchers.IO){
        val states = HashMap<StateAttribute, Any>()
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId, DEVICE_CON_TIMEOUT_MS)
        } catch (ex: Exception) {
            states[StateAttribute.Online] = false
            return@withContext states
        } catch (thr: Throwable) {
            states[StateAttribute.Online] = false
            return@withContext states
        }

        try {
            val on = readDeviceSwitchState(devicePtr)
            states[StateAttribute.Switch] = on

            val color = readDeviceColorState(devicePtr)
            states[StateAttribute.Color] = color

            val brightness = readDeviceLevelState(devicePtr)
            states[StateAttribute.Brightness] = brightness

            val colorTemperature = readDeviceColorTemperatureState(devicePtr)
            states[StateAttribute.ColorTemperature] = colorTemperature

            states[StateAttribute.Online] = true
        }catch (ex: Exception){
            Timber.e("Error happened while trying to read states: ${ex.localizedMessage}")
            states[StateAttribute.Online] = false
        }

        return@withContext states
    }

    /**
     * Fetches device's current state value from a certain attribute
     * @note think of online status as bonus state.  it will always be available
     *
     * @param deviceId device identifier
     * @param stateAttribute state attribute
     */
    @Deprecated(
        message = "Deprecated without a replacement due to its lack of usage" +
                "Please consider using readDeviceStates(deviceId, deviceType).",
        replaceWith = ReplaceWith("readDeviceStates(deviceId, deviceType)"),
        level = DeprecationLevel.ERROR
    )
    suspend fun readDeviceStates(
        deviceId: Long, stateAttribute: StateAttribute
    ): HashMap<StateAttribute, Any> = withContext(Dispatchers.IO){
        val states = HashMap<StateAttribute, Any>()
        try {
            MtrClient.getConnectedDevicePointer(context, deviceId, DEVICE_CON_TIMEOUT_MS)
        } catch (ex: Exception) {
            states[StateAttribute.Online] = false
            return@withContext states
        } catch (thr: Throwable) {
            states[StateAttribute.Online] = false
            return@withContext states
        }

        // Set online status
        states[StateAttribute.Online] = true
        return@withContext states
    }

    /**
     * Fetches device's current state value from a certain attribute
     * @note think of online status as bonus state.  it will always be available
     *
     * @param deviceId device identifier
     * @param deviceType device type
     * @param stateAttribute state attribute
     */
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun readDeviceStates(
        deviceId: Long, deviceType: DeviceType, stateAttribute: StateAttribute
    ): HashMap<StateAttribute, Any> = withContext(Dispatchers.IO){
        val states = HashMap<StateAttribute, Any>()
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId, DEVICE_CON_TIMEOUT_MS)
        } catch (ex: Exception) {
            states[StateAttribute.Online] = false
            return@withContext states
        } catch (thr: Throwable) {
            states[StateAttribute.Online] = false
            return@withContext states
        }

        val status = AttributeUtils.valStateAttribute(deviceType, stateAttribute)
        if(!status) {
            // ToDo() consider throwing an exception instead of returning an empty map
            Timber.d("Invalid state attribute for device type: $deviceType")
            states[StateAttribute.Online] = true
            return@withContext states
        }

        try {
            when(stateAttribute) {
                StateAttribute.Switch -> {
                    val on = readDeviceSwitchState(devicePtr)
                    states[StateAttribute.Switch] = on
                }
                StateAttribute.Color ->{
                    val color = readDeviceColorState(devicePtr)
                    states[StateAttribute.Color] = color
                }
                StateAttribute.ColorTemperature ->{
                    val colorTemperature = readDeviceColorTemperatureState(devicePtr)
                    states[StateAttribute.ColorTemperature] = colorTemperature
                }
                StateAttribute.Brightness ->{
                    val brightness = readDeviceLevelState(devicePtr)
                    states[StateAttribute.Brightness] = brightness
                }
                StateAttribute.FanSpeed ->{
                    val fanSpeed = readFanSpeed(devicePtr)
                    states[StateAttribute.FanSpeed] = fanSpeed
                }
                StateAttribute.FanMode -> {
                    val fanMode = readFanMode(devicePtr)
                    states[StateAttribute.FanMode] = fanMode
                }
                StateAttribute.Online->{
                    states[StateAttribute.Online] = true
                }
            }
            // Set online status
            states[StateAttribute.Online] = true
        }catch (ex: Exception){
            Timber.e("Error happened while trying to read states: ${ex.localizedMessage}")
            states[StateAttribute.Online] = false
        }

        return@withContext states
    }

    /**
     * Fetches device's current state value from a certain attribute
     * @note think of online status as bonus state.  it will always be available
     *
     * @param deviceId device identifier
     * @param deviceType device type
     * @param stateAttribute state attribute
     */
    suspend fun readDeviceStates(
        deviceId: Long, deviceType: Long, stateAttribute: StateAttribute
    ): HashMap<StateAttribute, Any> = withContext(Dispatchers.IO){
        val type = DeviceTypeUtil.toEnum(deviceType)
        return@withContext readDeviceStates(deviceId, type, stateAttribute)
    }

    /**
     * Fetches device's current state value from a certain attribute
     * @note think of online status as bonus state.  it will always be available
     *
     * @param deviceId device identifier
     * @param deviceType device type
     */
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun readDeviceStates(
        deviceId: Long,
        deviceType: DeviceType,
    ): HashMap<StateAttribute, Any> = withContext(Dispatchers.IO){
        val states = HashMap<StateAttribute, Any>()
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId, DEVICE_CON_TIMEOUT_MS)
        } catch (ex: Exception) {
            states[StateAttribute.Online] = false
            return@withContext states
        } catch (thr: Throwable) {
            states[StateAttribute.Online] = false
            return@withContext states
        }

        // Query device states
        when(deviceType) {
            DeviceType.OnOffLight ->{
                val onOffLightStates = readOnOffLightStates(devicePtr)
                states.putAll(onOffLightStates)
            }
            DeviceType.ColorTemperatureLight -> {
                val colorTemperatureStates = readColorTemperatureLightStates(devicePtr)
                states.putAll(colorTemperatureStates)
            }
            DeviceType.ExtendedColorLight ->{
                val extendedColorStates = readExtendedColorLightStates(devicePtr)
                states.putAll(extendedColorStates)
            }
            DeviceType.DimmableLight -> {
                val dimmableLightStates = readDimmableLightStates(devicePtr)
                states.putAll(dimmableLightStates)
            }
            DeviceType.Socket -> {
                val socketStates = readSocketStates(devicePtr)
                states.putAll(socketStates)
            }
            DeviceType.Fan ->{
                val fanStates = readFanStates(devicePtr)
                states.putAll(fanStates)
            }
            else -> {
                Timber.d("Nothing to do here")
            }
        }
        return@withContext states
    }

    /**
     * Fetches device's current state value from a certain attribute
     *
     * @param deviceId device identifier
     * @param deviceType device type
     */
    suspend fun readDeviceStates(
        deviceId: Long,
        deviceType: Long,
    ): HashMap<StateAttribute, Any> = withContext(Dispatchers.IO){
        val type = DeviceTypeUtil.toEnum(deviceType)
        return@withContext readDeviceStates(deviceId, type)
    }

    /**
     * Reads on off light states
     *
     * @param devicePtr connected device pointer
     */
    private suspend fun readOnOffLightStates(devicePtr: Long): HashMap<StateAttribute, Any>{
        val states = HashMap<StateAttribute, Any>()

        try {
            // read switch states
            val on = readDeviceSwitchState(devicePtr)
            states[StateAttribute.Switch] = on
            states[StateAttribute.Online] = true
        }catch (ex:Exception) {
            states[StateAttribute.Online] = false
        } catch (ex: Throwable) {
            states[StateAttribute.Online] = false
        }


        return states
    }

    /**
     * Reads color temperature light states
     *
     * @param devicePtr connected device pointer
     */
    private suspend fun readColorTemperatureLightStates(devicePtr: Long): HashMap<StateAttribute, Any>{
        val states = HashMap<StateAttribute, Any>()

        try {
            // read switch states
            val on = readDeviceSwitchState(devicePtr)
            states[StateAttribute.Switch] = on

            // read device color state
            val color = readDeviceColorState(devicePtr)
            states[StateAttribute.Color] = color

            // read brightness
            val brightness = readDeviceLevelState(devicePtr)
            states[StateAttribute.Brightness] = brightness

            // read color temperature state
            val colorTemperature = readDeviceColorTemperatureState(devicePtr)
            states[StateAttribute.ColorTemperature] = colorTemperature

            states[StateAttribute.Online] = true
        }catch (ex:Exception) {
            Timber.e(ex.localizedMessage)
            states[StateAttribute.Online] = false
        } catch (ex: Throwable) {
            Timber.e(ex.localizedMessage)
            states[StateAttribute.Online] = false
        }


        return states
    }

    /**
     * Reads extended color light states
     *
     * @param devicePtr connected device pointer
     */
    private suspend fun readExtendedColorLightStates(devicePtr: Long): HashMap<StateAttribute, Any>{
        val states = HashMap<StateAttribute, Any>()

        try {
            // read switch states
            val on = readDeviceSwitchState(devicePtr)
            states[StateAttribute.Switch] = on

            // read device color state
            val color = readDeviceColorState(devicePtr)
            states[StateAttribute.Color] = color

            // read brightness
            val brightness = readDeviceLevelState(devicePtr)
            states[StateAttribute.Brightness] = brightness

            // read color temperature state
            val colorTemperature = readDeviceColorTemperatureState(devicePtr)
            states[StateAttribute.ColorTemperature] = colorTemperature

            states[StateAttribute.Online] = true
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            states[StateAttribute.Online] = false
        } catch (ex: Throwable) {
            Timber.e(ex.localizedMessage)
            states[StateAttribute.Online] = false
        }


        return states
    }

    /**
     * Reads dimmable light states
     *
     * @param devicePtr connected device pointer
     */
    private suspend fun readDimmableLightStates(devicePtr: Long): HashMap<StateAttribute, Any>{
        val states = HashMap<StateAttribute, Any>()

        try {
            // read switch states
            val on = readDeviceSwitchState(devicePtr)
            states[StateAttribute.Switch] = on

            // read brightness
            val brightness = readDeviceLevelState(devicePtr)
            states[StateAttribute.Brightness] = brightness

            // read color temperature state
            val colorTemperature = readDeviceColorTemperatureState(devicePtr)
            states[StateAttribute.ColorTemperature] = colorTemperature
            states[StateAttribute.Online] = true
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
            states[StateAttribute.Online] = false
        } catch (ex: Throwable) {
            Timber.e(ex.localizedMessage)
            states[StateAttribute.Online] = false
        }

        return states
    }

    /**
     * Reads socket states
     *
     * @param devicePtr connected device pointer
     */
    private suspend fun readSocketStates(devicePtr: Long): HashMap<StateAttribute, Any>{
        val states = HashMap<StateAttribute, Any>()

        try {
            // read switch states
            val on = readDeviceSwitchState(devicePtr)
            states[StateAttribute.Switch] = on
            states[StateAttribute.Online] = true
        }catch (ex: Exception){
            states[StateAttribute.Online] = false
        } catch (ex: Throwable) {
            states[StateAttribute.Online] = false
        }

        return states
    }

    /**
     * Reads fan states
     *
     * @param devicePtr connected device pointer
     */
    private suspend fun readFanStates(devicePtr: Long): HashMap<StateAttribute, Any> {
        val states = HashMap<StateAttribute, Any>()

        try {
            // read mode states
            val fanMode = readFanMode(devicePtr)
            states[StateAttribute.FanMode] = fanMode

            // infer switch status from mode
            states[StateAttribute.Switch] = (fanMode.mode != FanMode.Off.mode)

            // read speed states
            val speed = readFanSpeed(devicePtr)
            states[StateAttribute.FanSpeed] = speed

            // set online status
            states[StateAttribute.Online] = true
        }catch (ex: Exception){
            Timber.e(ex.localizedMessage)
            states[StateAttribute.Online] = false
        }

        return states
    }

    /**
     * Reads and converts the mtr device's color data to standards HSV color space values
     * @param devicePtr connected device pointer
     */
    private suspend fun readDeviceColorState(devicePtr: Long): HSVColor {
        return try {
            val mtrHue = clusterManager.readCurrentHueAttribute(devicePtr, NODE_CTRL_ENDPOINT_ID)
            val mtrSaturation =
                clusterManager.readCurrentSaturationAttribute(devicePtr, NODE_CTRL_ENDPOINT_ID)
            HSVColor(
                hue = AttributeUtils.mapValue(mtrHue, MTR_MAX_HUE, STD_MAX_HUE),
                saturation = AttributeUtils
                    .mapValue(mtrSaturation, MTR_MAX_SATURATION, STD_MAX_SATURATION),
                value = STD_MAX_BRIGHTNESS
            )
        } catch (ex: Exception) {
            Timber.e("failed to read current hue and saturation attributes. Cause")
            throw ex
        } catch (ex: Throwable) {
            throw ReadClusterAttributeException(ex.localizedMessage)
        }
    }

    /**
     * Reads device's switch state
     * @param devicePtr connected device pointer
     */
    private suspend fun readDeviceSwitchState(devicePtr: Long): Boolean {
        return  try {
            clusterManager.readOnOffAttribute(devicePtr, NODE_CTRL_ENDPOINT_ID)
        }catch (ex: Exception) {
            Timber.e("failed to read current switch attributes. Cause: ${ex.localizedMessage}")
            throw ex
        } catch (ex: Throwable) {
            throw ReadClusterAttributeException(ex.localizedMessage)
        }
    }

    /**
     * Reads device's brightness/level state
     * @param devicePtr connected device pointer
     */
    private suspend fun readDeviceLevelState(devicePtr: Long): Int {
        return try {
            val mtrBrightness =
                clusterManager.readCurrentLevelAttribute(devicePtr, NODE_CTRL_ENDPOINT_ID)
            AttributeUtils.mapValue(mtrBrightness, MTR_MAX_BRIGHTNESS, STD_MAX_BRIGHTNESS)
        } catch (ex: Exception) {
            Timber.e("failed to read current level attributes")
            throw ex
        } catch (ex: Throwable) {
            throw ReadClusterAttributeException(ex.localizedMessage)
        }
    }

    /**
     * Reads device's color temperature state
     * @param devicePtr connected device pointer
     */
    private suspend fun readDeviceColorTemperatureState(devicePtr: Long): Int {
        return try {
            val mtrColorTemperature =
                clusterManager.readColorTemperatureAttribute(devicePtr, NODE_CTRL_ENDPOINT_ID)
            AttributeUtils.mapValue(
                mtrColorTemperature,
                MTR_MIN_COLOR_TEMPERATURE,
                MTR_MAX_COLOR_TEMPERATURE,
                STD_MIN_COLOR_TEMPERATURE,
                STD_MAX_COLOR_TEMPERATURE
            )
        } catch (ex: Exception) {
            Timber.e("failed to read current level attributes")
            throw ex
        } catch (ex: Throwable) {
            throw ReadClusterAttributeException(ex.localizedMessage)
        }
    }

    /**
     * Reads fan's mode value
     * @param devicePtr connected device pointer
     */
    private suspend fun readFanMode(devicePtr: Long): FanMode {
        return try {
            val mtrFanMode = clusterManager.readFanMode(devicePtr, NODE_CTRL_ENDPOINT_ID)
            FanModeUtil.toEnum(mtrFanMode)
        } catch (ex: Exception) {
            Timber.e("failed to read fan mode attribute")
            throw ex
        }
    }

    /**
     * Reads fan's current speed value
     * @param devicePtr connected device pointer
     */
    private suspend fun readFanSpeed(devicePtr: Long): Int {
        return try {
            clusterManager.readFanPercentageSetting(devicePtr, NODE_CTRL_ENDPOINT_ID)
        } catch (ex: Exception) {
            Timber.e("failed to read fan speed attribute")
            throw ex
        }
    }

    /**
     * Subscribes to all the changes in device's attributes
     *
     * @param deviceId device identifier
     * @param listener subscription listener
     */
    suspend fun subscribe(deviceId: Long, listener: DeviceSubscriptionListener) {
        // add device state change task
        return suspendCoroutine {
            // stop jobs
            stopDevicePingJob()

            // add device to state change task map
            stateChangeTasks[deviceId] = DeviceStateChangeTask(listener = listener)

            // start device ping jobs
            startDevicePingJob()
        }
    }

    /**
     * Subscribes to all the changes in device's attributes
     *
     * @param deviceId device identifier
     * @param listener subscription listener
     */
    private suspend fun subscribeToPath(
        deviceId: Long, listener: DeviceSubscriptionListener
    ): Unit = withContext(Dispatchers.Default){
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        } catch (ex: Exception) {
            listener.onError(ex)
            return@withContext
        } catch (thr: Throwable) {
            listener.onError(DeviceUnreachableException(thr.localizedMessage))
            return@withContext
        }

        val subscriptionEstCallback = SubscriptionEstablishedCallback { subscriptionId ->
            Timber.d("Subscription to device est: $subscriptionId")
            listener.onSubscriptionEstablished(subscriptionId.toULong())
        }

        val reSubscriptionAttemptCallback =
            ResubscriptionAttemptCallback{ terminationCause, nextIntervalMs ->
                val msg = "Re-subscription attempt in $nextIntervalMs ms. Cause: $terminationCause"
                Timber.d(msg)
            }

        // subscribe to device state changes
        MtrClient.getDeviceController(context).subscribeToPath(
            subscriptionEstCallback,
            reSubscriptionAttemptCallback,
            getReportCallback(listener),
            devicePtr,
            getAttributePathList(),
            ArrayList<ChipEventPath>(),
            MIN_SUB_INTERVAL_MS,
            MAX_SUB_INTERVAL_MS,
            false,
            false,
            0
        )
    }

    /**
     * Builds attribute path list
     */
    private fun getAttributePathList(): ArrayList<ChipAttributePath> {
        // Create attribute path
        val attributePath = ChipAttributePath.newInstance(
            ChipPathId.forId(NODE_CTRL_ENDPOINT_ID.toLong()),
            ChipPathId.forWildcard(),
            ChipPathId.forWildcard()
        )

        val attributePaths = ArrayList<ChipAttributePath>()
        attributePaths.add(attributePath)
        return  attributePaths
    }

    /**
     * Builds report callback
     *
     * @param listener device subscription listener
     */
    private fun getReportCallback(listener: DeviceSubscriptionListener): ReportCallback {
        return object : ReportCallback {
            override fun onError(
                attributePath: ChipAttributePath?,
                eventPath: ChipEventPath?,
                ex: Exception
            ) {
                Timber.e("Failed to report node state. Cause: ${ex.localizedMessage}")
                listener.onError(OperationFailureException("Failed to fetch device states"))
            }

            override fun onReport(nodeState: NodeState?) {
                val states = ClusterUtils.handleNodeStateReport(nodeState)
                listener.onReport(states)
            }
        }
    }

    /**
     * Unsubscribe from all subscriptions
     */
    suspend fun unsubscribe() {
        return suspendCoroutine {
            // shutdown subscriptions
            MtrClient.getDeviceController(context).shutdownSubscriptions()
            // stop device pings
            stopDevicePingJob()
            // clear tasks
            clearDevicePingTasks()
        }
    }

    /**
     * Unsubscribe from all subscriptions with a certain device
     *
     * @param deviceId device identifier
     */
    suspend fun unsubscribe(deviceId: Long) {
        // remove device from map
        removeStateChangeTask(deviceId)

        // shutdown subscription
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        } catch (ex: Exception) {
            return
        } catch (thr: Throwable) {
            return
        }

        val fabricIndex = clusterManager.readCurrentFabricIndex(devicePtr)
        MtrClient.getDeviceController(context).shutdownSubscriptions(
            fabricIndex.toInt(), deviceId
        )
    }

    /**
     * Unsubscribe from a subscription with a certain device
     *
     * @param deviceId device identifier
     * @param subscriptionId subscription identifier
     */
    suspend fun unsubscribe(deviceId: Long, subscriptionId: ULong) {
        // remove device from map
        removeStateChangeTask(deviceId)

        // shutdown subscription
        val devicePtr = try {
            MtrClient.getConnectedDevicePointer(context, deviceId)
        } catch (ex: Exception) {
            return
        } catch (thr: Throwable) {
            return
        }

        val fabricIndex = clusterManager.readCurrentFabricIndex(devicePtr)
        MtrClient.getDeviceController(context).shutdownSubscriptions(
            fabricIndex.toInt(), deviceId, subscriptionId.toLong()
        )
    }

    /**
     * Stop device ping job
     */
    private fun stopDevicePingJob() {
        // clear the state change job
        stateChangeJob?.cancel()
        stateChangeJob = null
    }

    /**
     * Launch device state change job
     */
    private fun launchStateChangeJob(): Job {
        val delayMillis = calculatePingJobDelay(stateChangeTasks.size)
        return stateChangeJobScope.launch {
            Timber.d("active status:| $isActive, period:| $delayMillis")
            while (isActive) {
                launch {
                    pingDevices()
                }
                delay(delayMillis)
            }
        }
    }

    /**
     * Start device ping job
     */
    private fun startDevicePingJob(){
        // launch state change job
        stateChangeJob = launchStateChangeJob()
    }

    /**
     * Clear device ping tasks
     */
    private fun clearDevicePingTasks() {
        // clear tasks
        stateChangeTasks.clear()
    }

    /**
     * Remove device state change task
     *
     * @param deviceId device identifier
     */
    private fun removeStateChangeTask(deviceId: Long) {
        // stop jobs
        stopDevicePingJob()

        // remove task
        stateChangeTasks.remove(deviceId)

        // start device ping jobs
        startDevicePingJob()
    }

    /**
     * Ping devices
     */
    private fun pingDevices() {
        stateChangeJobScope.launch {
            stateChangeTasks.forEach { (deviceId, task) ->
                // ping device
                var status = false
                try {
                    status = isDeviceOnline(deviceId, DEVICE_PING_TIMEOUT_MS)
                }catch (ex: Exception) {
                    Timber.e(ex.localizedMessage)
                } catch (thr: Throwable) {
                    Timber.e(thr.localizedMessage)
                }

                // check status change
                if(task.timeouts >= MAX_DEVICE_PINGS && status){
                    Timber.d("Device just came back online:| $deviceId")
                    pushOnlineStatus(true, task.listener)
                    // re-subscribe
                    subscribeToPath(deviceId, task.listener)
                }else if(task.timeouts == (MAX_DEVICE_PINGS - 1) && !status){
                    Timber.e("Device just went offline:| $deviceId")
                    pushOnlineStatus(false, task.listener)
                }

                // update timeouts
                task.timeouts = updatePingTimeouts(status, task.timeouts)
            }
        }
    }

    /**
     * Update timeouts
     *
     * @param status ping status
     * @param timeouts current timeouts
     */
    private fun updatePingTimeouts(status: Boolean, timeouts: Int): Int {
        return when(status) {
            true -> 0
            false -> {
                if(timeouts < (MAX_DEVICE_PINGS + 1)) {
                    timeouts + 1
                }else{
                    timeouts
                }
            }
        }
    }

    /**
     * Calculates the delay for device ping job
     *
     * @param taskCount number tasks
     */
    private fun calculatePingJobDelay(taskCount: Int): Long{
        val mean = 25
        val deviation = 18
        val factor = 30 * (
                1.0 + Erf.erf((taskCount.toDouble() - mean)/(deviation * sqrt(2.0)))
        )
        return round(factor * 1000).toLong()
    }

    /**
     * Pushes the online status
     *
     * @param status online status
     * @param listener device listener
     */
    private fun pushOnlineStatus(status: Boolean, listener: DeviceSubscriptionListener) {
        val states = hashMapOf<StateAttribute, Any>()
        states[StateAttribute.Online] = status
        listener.onReport(states)
    }

    companion object {
        /**
         * Matter device ctrl default endpoint identifier
         */
        private const val NODE_CTRL_ENDPOINT_ID = 1

        /**
         * Min subscription time in milliseconds
         */
        private const val MIN_SUB_INTERVAL_MS = 1

        /**
         * Max subscription time in milliseconds
         */
        private const val MAX_SUB_INTERVAL_MS = 60

        /**
         * Connected device pointer timeout
         */
        private const val DEVICE_CON_TIMEOUT_MS = 15000L

        /**
         * Max device pings
         */
        private const val MAX_DEVICE_PINGS = 1

        /**
         * Device ping timeout
         */
        private const val DEVICE_PING_TIMEOUT_MS = 1000L
    }
}
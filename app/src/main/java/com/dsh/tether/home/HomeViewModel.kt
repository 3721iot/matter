package com.dsh.tether.home

import android.nfc.NdefRecord
import androidx.lifecycle.*
import com.dsh.data.model.device.Device
import com.dsh.data.model.device.DeviceState
import com.dsh.data.model.device.DeviceStates
import com.dsh.data.model.device.Devices
import com.dsh.data.repository.DeviceStatesRepo
import com.dsh.data.repository.DevicesRepo
import com.dsh.data.repository.UserProfileRepo
import com.dsh.matter.management.device.DeviceController
import com.dsh.matter.management.device.DeviceStatesManager
import com.dsh.matter.management.device.DeviceSubscriptionListener
import com.dsh.matter.model.color.HSVColor
import com.dsh.matter.model.device.DeviceType
import com.dsh.matter.model.device.FanMode
import com.dsh.matter.model.device.StateAttribute
import com.dsh.matter.util.device.FanModeUtil
import com.dsh.matter.util.device.UnsupportedDeviceTypeException
import com.dsh.openai.home.InferenceEngine
import com.dsh.openai.home.model.HomeDevice
import com.dsh.openai.home.model.HomeDeviceStatesBuilder
import com.dsh.openai.home.model.InferenceResult
import com.dsh.speech.synthesizer.SpeechSynthesizer
import com.dsh.tether.model.MtrDeviceMetadata
import com.dsh.tether.model.TetherDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val devicesRepo: DevicesRepo,
    private val deviceStatesRepo: DeviceStatesRepo,
    private val userProfileRepo: UserProfileRepo,
    private val deviceStates: DeviceStatesManager,
    private val deviceController: DeviceController
) : ViewModel() {

    /**
     * Inference result segments
     */
    private val _resultChunks = mutableListOf<String>()
    private val _resultSegments: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    private val _isBusy = AtomicBoolean()
    private val _isStreamComplete = AtomicBoolean()

    /**
     * Setup payload live data
     */
    private val _setupPayload = MutableLiveData<String?>()
    val setupPayload : LiveData<String?>
        get() = _setupPayload

    /**
     * Initial setup event which triggers the [HomeFragment] to get the data required for its UI
     */
    init {
        liveData { emit(devicesRepo.getAllDevices()) }
        liveData { emit(deviceStatesRepo.getAllDeviceStates()) }
        liveData { emit(userProfileRepo.getUserProfile()) }
    }

    /**
     * Reference device, devices states and user prefs Flows
     */
    private val devicesFlow = devicesRepo.devicesFlow
    private val userProfileFlow = userProfileRepo.userProfileFlow
    private val devicesStateFlow = deviceStatesRepo.deviceStatesFlow

    /**
     * Recreate the devices list everytime the list of devices and states are updated
     */
    private val thrDevicesFlow =
        combine(devicesFlow, devicesStateFlow) {
                devices: Devices,
                deviceStates: DeviceStates ->
            return@combine processDevices(
                devices = devices,
                deviceStates = deviceStates
            )
        }

    /**
     * Tether devices live data
     */
    val thrDevicesLiveData = thrDevicesFlow.asLiveData()

    /**
     * The user profile live data
     */
    val userProfileLiveData = userProfileFlow.asLiveData()

    /**
     *  Process device list and device states changes
     *  @param devices Proto devices
     *  @param deviceStates Proto devices states
     *
     *  @return the list of devices
     */
    private fun processDevices(
        devices: Devices,
        deviceStates: DeviceStates
    ) : List<TetherDevice> {
        val uiDevices = ArrayList<TetherDevice>()
        devices.devicesList.forEach{device->
            val state = deviceStates.deviceStatesList.find { it.deviceId == device.deviceId }
            val metadata = buildMtrDeviceMetadata(device)
            val thrDevice = when(state == null) {
                true -> {
                    val statesMap = formatDeviceState(device.deviceType)
                    buildTetherDevice(device, statesMap, metadata)
                }
                false->{
                    val statesMap = formatDeviceState(device.deviceType, state)
                    buildTetherDevice(device, statesMap, metadata)
                }
            }
            uiDevices.add(thrDevice)
        }
        return uiDevices
    }

    /**
     * Builds device metadata
     *
     * @return the device metadata
     */
    private fun buildMtrDeviceMetadata(device: Device) : MtrDeviceMetadata {
        // ToDo() use mapping functions instead of this forEach
        val capabilityList = HashSet<Int>()
        device.metadata.discoveryCapabilitiesList.forEach {
            capabilityList.add(it)
        }

        return MtrDeviceMetadata(
            vendorId = device.vendorId.toInt(),
            productId = device.productId.toInt(),
            version = device.metadata.version,
            discriminator = device.metadata.discriminator,
            setupPinCode = device.metadata.setupPinCode,
            commissioningFlow = device.metadata.commissioningFlow,
            hasShortDiscriminator = device.metadata.hasShortDiscriminator,
            discoveryCapabilities = capabilityList
        )
    }

    /**
     * Builds the tether device
     *
     * @return the tether device
     */
    private fun buildTetherDevice(
        device: Device,
        states: HashMap<StateAttribute, Any>,
        metadata: MtrDeviceMetadata
    ): TetherDevice {
        return TetherDevice(
            id = device.deviceId,
            name = device.name,
            home = "Someone",
            room = device.room,
            type = device.deviceType.toString(),
            states = states,
            metadata = metadata
        )
    }

    /**
     * Formats device state
     *
     * @param deviceType device type
     * @return the device states
     */
    private fun formatDeviceState(deviceType: Long ): HashMap<StateAttribute, Any> {
        val statesMap = HashMap<StateAttribute, Any>()
        statesMap[StateAttribute.Switch] = false
        statesMap[StateAttribute.Online] = true
        when(deviceType) {
            DeviceType.Fan.type -> {
                statesMap[StateAttribute.FanMode] = FanMode.Off
            }
            DeviceType.OnOffLight.type,
            DeviceType.DimmableLight.type,
            DeviceType.ExtendedColorLight.type,
            DeviceType.ColorTemperatureLight.type-> {
                statesMap[StateAttribute.Brightness] = 0
            }
        }
        return statesMap
    }

    /**
     * Formats device state
     *
     * @param deviceType device type
     * @return the device states
     */
    private fun formatDeviceState(deviceType: Long, deviceState: DeviceState): HashMap<StateAttribute, Any> {
        val statesMap = HashMap<StateAttribute, Any>()
        statesMap[StateAttribute.Switch] = deviceState.on
        statesMap[StateAttribute.Online] = deviceState.online
        when(deviceType) {
            DeviceType.Fan.type -> {
                statesMap[StateAttribute.FanMode] = FanModeUtil.toEnum(deviceState.fanMode)
            }
            DeviceType.OnOffLight.type,
            DeviceType.DimmableLight.type,
            DeviceType.ExtendedColorLight.type,
            DeviceType.ColorTemperatureLight.type-> {
                statesMap[StateAttribute.Brightness] = deviceState.brightness
            }
        }
        return statesMap
    }

    /**
     * Updates switch status
     *
     * @param deviceId device identifier
     * @param deviceType device type
     * @param on power status
     */
    fun updateDeviceSwitchStatus(deviceId: Long, deviceType: String, on: Boolean) {
        viewModelScope.launch {
            if(deviceType.toLong() == DeviceType.Fan.type) {
                // change fan mode
                val fanMode = if (on) FanMode.High else FanMode.Off
                deviceController.fanMode(deviceId = deviceId, fanMode = fanMode)
            }else {
                // switch the device
                deviceController.power(deviceId = deviceId, on = on)
            }
            // update states repo
            deviceStatesRepo.updateDeviceState(deviceId = deviceId, online = true, on = on)
        }
    }

    /**
     * Updates switch status
     *
     * @param deviceId device identifier
     * @param on power status
     */
    fun updateDeviceSwitchStatus(deviceId: Long, on: Boolean) {
        viewModelScope.launch {
            // switch the device
            deviceController.power(deviceId = deviceId, on = on)
            // update states repo
            deviceStatesRepo.updateDeviceState(deviceId = deviceId, online = true, on = on)
        }
    }

    /**
     * Initialize device states
     */
    fun initDeviceStates() {
        viewModelScope.launch {
            val devices = devicesRepo.getAllDevices().devicesList
            devices.forEach { device ->
                if(device.deviceId != -1L){
                    return@forEach
                }
                try {
                    val deviceStates =
                        deviceStates.readDeviceStates(
                            deviceId = device.deviceId,
                            deviceType = device.deviceType
                        )
                    if(deviceStates.isEmpty()){
                        deviceStatesRepo
                            .updateDeviceState(deviceId = device.deviceId, online = false)
                        return@forEach
                    }
                    updateStates(device.deviceId, deviceStates)
                }catch (ex: UnsupportedDeviceTypeException){
                    Timber.e(ex.localizedMessage)
                } catch (ex: Exception) {
                    Timber.e(ex,"Something is not right. Cause: ${ex.localizedMessage}")
                }
            }
        }
    }

    /**
     * Handles the Ndefs records
     *
     * @param records ndef records
     */
    fun handleNdefRecords(records: Array<NdefRecord>?) {
        if(null == records || records.size != 1){
            return
        }

        // check if NDEF URI record starts with "mt:"
        val uri = records[0].toUri()
        if (!uri?.scheme.equals("mt", true)) {
            return
        }

        val payload = uri.toString().uppercase()
        _setupPayload.postValue(payload)
    }

    /**
     * Reset the payload value
     */
    fun consumePayload() {
        _setupPayload.value = null
    }

    /**
     * Update device states
     *
     * @param deviceId device identifier
     */
    private fun updateStates(deviceId: Long, deviceStates :HashMap<StateAttribute, Any>){
        viewModelScope.launch {
            try {
                deviceStatesRepo.updateDeviceState(
                    deviceId = deviceId,
                    online = if(deviceStates[StateAttribute.Online] != null) {
                        deviceStates[StateAttribute.Online] as Boolean
                    }else{false},
                    on = if (deviceStates[StateAttribute.Switch] != null) {
                        deviceStates[StateAttribute.Switch] as Boolean
                    }else { null },
                    hue = if (deviceStates[StateAttribute.Color] != null) {
                        (deviceStates[StateAttribute.Color] as HSVColor).hue
                    }else{ null},
                    saturation = if(deviceStates[StateAttribute.Color] != null) {
                        (deviceStates[StateAttribute.Color] as HSVColor).saturation
                    }else{null},
                    brightness = if(deviceStates[StateAttribute.Brightness] != null) {
                        deviceStates[StateAttribute.Brightness] as Int
                    }else{null},
                    colorTemperature = if(deviceStates[StateAttribute.Brightness] != null) {
                        deviceStates[StateAttribute.Brightness] as Int
                    }else{null},
                    fanMode = if(deviceStates[StateAttribute.FanMode] != null) {
                        (deviceStates[StateAttribute.FanMode] as FanMode).mode
                    }else{null},
                    fanSpeed = if(deviceStates[StateAttribute.FanSpeed] != null) {
                        deviceStates[StateAttribute.FanSpeed] as Int
                    }else{null}
                )
            }catch (ex: UnsupportedDeviceTypeException){
                Timber.e(ex.localizedMessage)
            } catch (ex: Exception) {
                Timber.e(ex,"Something is not right. Cause: ${ex.localizedMessage}")
            }
        }
    }

    /**
     * Start monitoring device states
     */
    fun startStatesMonitoring() {
        viewModelScope.launch {
            val devices = devicesRepo.getAllDevices().devicesList
            devices.forEach { device ->
                if(device.deviceId != -1L){
                    return@forEach
                }
                val listener = object : DeviceSubscriptionListener() {
                    override fun onError(ex: Exception) {
                        Timber.e("Failed to monitor states. Cause: ${ex.localizedMessage}")
                    }

                    override fun onSubscriptionEstablished(subscriptionId: ULong) {
                        Timber.d("Subscription established: $subscriptionId")
                    }

                    override fun onReport(report: HashMap<StateAttribute, Any>) {
                        updateStates(device.deviceId, report)
                    }
                }
                deviceStates.subscribe(device.deviceId, listener)
            }
        }
    }

    /**
     * Stop monitoring device states
     */
    fun stopStatesMonitoring() {
        viewModelScope.launch {
            try{
                deviceStates.unsubscribe()
            }catch (ex: Exception) {
                Timber.e("Failed to shutdown subscriptions")
            }
        }
    }

    fun handleResult(synthesizer: SpeechSynthesizer, result: String) {
        viewModelScope.launch {
            synthesizer.speakAsync(result)
                .addOnSuccessListener { status ->
                    Timber.d("You should see this at some point| status: $status")
                }.addOnFailureListener { exception ->
                    Timber.e(exception, "Successful failure")
                }
        }
    }

    fun handleResult(synthesizer: SpeechSynthesizer, data: String, stream: Boolean, complete: Boolean) {
        if (synthesizer != null) {

            viewModelScope.launch {
                val delimiters = "[,.:;?!](?=\\s)".toRegex()
                val regexSet = setOf(",", ".", ";", ":", "?", "!")
                if(stream){
                    val splits = data.split(delimiters).toList()
                    splits.forEach{split ->
                        _resultChunks.add(split)
                        val isMatched = regexSet.any { split.contains(it) }
                        if(isMatched){
                            val segment = _resultChunks.joinToString("").trim()
                            _resultSegments.add(segment)
                            _resultChunks.clear()
                            if(!_isBusy.get()){
                                _isBusy.set(true)
                                synthesize(synthesizer)
                            }
                        }
                    }
                }else{
                    _resultSegments.add(data)
                }
                _isStreamComplete.set(complete)
            }
        }
    }

    private fun synthesize(synthesizer: SpeechSynthesizer) {
        viewModelScope.launch {
            if(_resultSegments.isEmpty() && _isStreamComplete.get()){
                _isBusy.set(false)
                return@launch
            }

            val segment = _resultSegments.poll()
            if(!segment.isNullOrBlank()){
                synthesizer.speakAsync(segment).addOnCompleteListener {
                    Timber.d("Speech completed")
                }
            }
            synthesize(synthesizer)
        }
    }

    fun handleIntent(inferenceEngine: InferenceEngine, intent: String) {
        if (inferenceEngine != null) {
            viewModelScope.launch {
                inferenceEngine.infer(intent, true)
            }
        }
    }

    /**
     * Converts Tether devices to Home devices
     *
     * @return the list of home devices
     */
    fun getHomeDevices(): List<HomeDevice> {
        return thrDevicesLiveData.value!!.map { thrDevice->
            val deviceStates = HomeDeviceStatesBuilder()
                .setPower(thrDevice.states[StateAttribute.Switch] as Boolean)
                .build()
            HomeDevice(
                deviceId = thrDevice.id.toString(),
                deviceType = thrDevice.type,
                deviceName = thrDevice.name,
                roomName = thrDevice.room,
                homeName = thrDevice.home,
                deviceStates = deviceStates
            )
        }
    }
}
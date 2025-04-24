package com.dsh.tether.home

import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.dsh.data.model.auth.UserProfile
import com.dsh.data.repository.DeviceStatesRepo
import com.dsh.data.repository.DevicesRepo
import com.dsh.matter.model.device.StateAttribute
import com.dsh.openai.home.HomeCommandInference.Companion.getClient
import com.dsh.openai.home.InferenceEngine
import com.dsh.openai.home.InferenceResultListener
import com.dsh.openai.home.InferenceToolsListener
import com.dsh.openai.home.model.ControlIntent
import com.dsh.openai.home.model.HomeDevice
import com.dsh.openai.home.model.HomeInfo
import com.dsh.openai.home.model.HomeLocation
import com.dsh.openai.home.model.InferenceResult
import com.dsh.openai.home.model.RoomInfo
import com.dsh.openai.home.model.automation.AutomationInfo
import com.dsh.openai.home.model.automation.Condition
import com.dsh.openai.home.model.automation.MatchType
import com.dsh.openai.home.model.automation.Task
import com.dsh.openai.home.model.config.InferenceConfigBuilder
import com.dsh.openai.home.model.config.ModelIdentifier
import com.dsh.speech.synthesizer.SpeechSynthesis
import com.dsh.speech.synthesizer.SpeechSynthesizer
import com.dsh.tether.BuildConfig
import com.dsh.tether.R
import com.dsh.tether.databinding.DialogUserIntentBinding
import com.dsh.tether.databinding.FragmentHomeBinding
import com.dsh.tether.device.SelectedDeviceViewModel
import com.dsh.tether.home.adapter.HomeDevicesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.log

@AndroidEntryPoint
class HomeFragment : Fragment(){

    /**
     * Inject dependency repos
     */
    @Inject internal lateinit var devicesRepo : DevicesRepo
    @Inject internal lateinit var devicesStateRepo: DeviceStatesRepo

    /**
     * [HomeFragment] binding
     */
    private lateinit var binding: FragmentHomeBinding

    /**
     * [HomeFragment]'s view model
     */
    private val viewModel: HomeViewModel by viewModels()

    /**
     * [SelectedDeviceViewModel] holds the current selected device info
     */
    private val selectedDeviceViewModel : SelectedDeviceViewModel by activityViewModels()

    /**
     * Inference engine
     */
//    private lateinit var inferenceEngine: InferenceEngine

    /**
     * User intent dialog
     */
    private lateinit var userIntentDialog : AlertDialog
    private lateinit var userIntentBinding: DialogUserIntentBinding

    /**
     * The synthesizer
     */
//    private lateinit var synthesizer: SpeechSynthesizer

    /**
     * Home device list recycler view adapter
     */
    private val homeDevicesAdapter =
        HomeDevicesAdapter(
            { thrDevice ->
                val on = if(thrDevice.states.containsKey(StateAttribute.Switch)) {
                    thrDevice.states[StateAttribute.Switch] as Boolean
                }else{
                    return@HomeDevicesAdapter
                }
                viewModel.updateDeviceSwitchStatus(thrDevice.id, thrDevice.type, !on)
            },
            { thrDevice ->
                // update selected device
                // navigate to device fragment
                selectedDeviceViewModel.setDevice(thrDevice)
                val action = HomeFragmentDirections.actionHomeToDeviceControllerFragment()
                requireView().findNavController().navigate(action)
            }
        )

    /**
     * Handles NFC tags containing matter payload.
     * Require 1 NDEF message containing 1 NDEF record
     */
    @SuppressWarnings("all")
    private fun onNfcIntent() {
        val messages = requireActivity().intent
            ?.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if ((null == messages) || (messages.size != 1)) {
            return
        }

        // clear intent extra
        requireActivity().intent.removeExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

        val ndefRecords = (messages[0] as NdefMessage).records
        viewModel.handleNdefRecords(ndefRecords)
    }

    override fun onCreateView(
        inflater: LayoutInflater,container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")

        // Bind home fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Bind user intent alert dialog
        userIntentBinding =
            DialogUserIntentBinding.inflate(inflater, container, false)

        // Setup inference engine
        setupInferenceEngine()

        // Setup speech synthesizer
        setupSpeechSynthesizer()

        // Setup Ui Elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        return binding.root
    }

    private fun setupSpeechSynthesizer() {
//        val serviceRegion = BuildConfig.AZURE_SPEECH_REGION
//        val speechSubscriptionKey = BuildConfig.AZURE_SPEECH_KEY
//        if (!TextUtils.isEmpty(serviceRegion) && !TextUtils.isEmpty(speechSubscriptionKey)) {
//            synthesizer = SpeechSynthesis.getClient(speechSubscriptionKey, serviceRegion)
//        } else {
//            Timber.d("serviceRegion  or speechSubscriptionKey not empty,please config Microsoft Azure")
//        }
    }

    private fun setupInferenceEngine() {
        // Initialize the inference engine
//        if (!TextUtils.isEmpty(BuildConfig.OPENAI_API_HOST) && !TextUtils.isEmpty(BuildConfig.OPENAI_API_KEY)) {
//            val config = InferenceConfigBuilder()
//                .setBaseUrl(BuildConfig.OPENAI_API_HOST)
//                .setToken(BuildConfig.OPENAI_API_KEY)
//                .setModelId(ModelIdentifier.FourPointZero)
//                .build()
//
//            inferenceEngine = run {
//                getClient(
//                    requireContext(),
//                    config,
//                    getInferenceToolsListener(),
//                    getInferenceResultListener()
//                )
//            }
//        } else {
//            Timber.d("OPENAI_API_HOST  or OPENAI_API_KEY not empty,please config AI")
//        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Do some work within the Android lifecycle
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                Timber.d("[*** LifeCycle ***] : onResume")
                // This will force an update on the device states
                if (requireActivity().intent?.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
                    onNfcIntent()
                }else{
                    viewModel.startStatesMonitoring()
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                Timber.d("[*** LifeCycle ***] : onPause")
                viewModel.stopStatesMonitoring()
            }
        })

//        // Add inference engine to life
//        if (inferenceEngine != null) {
//
//            viewLifecycleOwner.lifecycle.addObserver(inferenceEngine)
//        }
//
//        // Add synthesizer to life
//        if (synthesizer != null) {
//            viewLifecycleOwner.lifecycle.addObserver(synthesizer)
//        }
    }

    private fun setupUiLayerElements() {
        Timber.d("Setting up UI layer elements")
        // Setup add device button
        binding.ivAddDevice.setOnClickListener {
            // Launch the intent
            val action = HomeFragmentDirections.actionHomeToPayloadReaderFragment()
            requireView().findNavController().navigate(action)
        }

        // Setup device list recycler view
        binding.lrvDevices.adapter = homeDevicesAdapter

        // Navigate to user profile
        binding.ivAvatar.setOnClickListener {view ->
            val action = HomeFragmentDirections.actionHomeToUserProfileFragment()
            view.findNavController().navigate(action)
        }

        // Initialize user intent dialog
        userIntentDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(userIntentBinding.root)
            .setCancelable(false)
            .setTitle("Input text command")
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val intent = userIntentBinding.etUserIntent.text.toString()
//                viewModel.handleIntent(inferenceEngine, intent)
            }.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .create()

        // Set settings icon listener
        binding.ivSettings.setOnClickListener {
            userIntentBinding.etUserIntent.setText("")
            userIntentDialog.show()
        }
    }

    private fun setupObservers() {
        Timber.d("Setting up observers")
        // Observe device list live data
        viewModel.thrDevicesLiveData.observe(viewLifecycleOwner) { thrDevices ->
            homeDevicesAdapter.submitList(thrDevices)
        }

        // Observe device setup payload
        viewModel.setupPayload.observe(viewLifecycleOwner) { payload ->
            if(null == payload) {
                return@observe
            }
            val action =
                HomeFragmentDirections.actionHomeToDeviceProvisioningFragment(payload)
            requireView().findNavController().navigate(action)

            // consume the payload to prevent triggering device commissioning again
            viewModel.consumePayload()
        }

        // Observe user profile live data
        viewModel.userProfileLiveData.observe(viewLifecycleOwner) { userProfile ->
            if(null == userProfile){
                return@observe
            }

            updateUserInfo(userProfile)
        }
    }

    private fun updateUserInfo(userProfile: UserProfile) {
        // set home name
        val homeName = "${userProfile.name}'s"
        binding.tvHomeName.text = homeName

        // set profile
        Glide.with(this)
            .load(userProfile.pictureUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_dot_filled)
            .into(binding.ivAvatar)
    }

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    private fun getInferenceResultListener(): InferenceResultListener {
        return object : InferenceResultListener {
            /**
             * Invoked on a successful intent inference completion
             *
             * @param data the inference result
             * @param stream whether the data is a stream or not.
             * @param complete whether the inference is completed or not.
             */
            override fun onCompletion(data: String, stream: Boolean, complete: Boolean) {
                requireActivity().runOnUiThread {
//                    viewModel.handleResult(synthesizer, data, stream, complete)
                }
            }

            /**
             * Invoked when inference is completed with an error
             * @param exception the exception
             */
            override fun onError(exception: Exception) {
                Timber.e("Something went wrong: ${exception.localizedMessage}")
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        exception.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getInferenceToolsListener(): InferenceToolsListener {
        return object : InferenceToolsListener {
            /**
             * Invoked to pass the device control arguments
             *
             * @param deviceIds the list of device identifiers
             * @param intent the device control intent
             * @param value the device control value
             */
            override fun onDeviceControl(
                deviceIds: List<String>,
                intent: ControlIntent,
                value: String
            ): Boolean {
                when(intent) {
                    ControlIntent.Brightness -> {
                        val brightness = value.toInt()
                        deviceIds.forEach { deviceId ->
                            Timber.d("deviceId=$deviceId | brightness=$brightness")
                        }
                    }
                    ControlIntent.Power -> {
                        val power = value.toBoolean()
                        deviceIds.forEach { deviceId ->
                            Timber.d("deviceId=$deviceId | power=$power")
                            viewModel.updateDeviceSwitchStatus(
                                deviceId.toLong(), power
                            )
                        }
                    }
                    ControlIntent.ColorTemperature -> {
                        val colorTemperature = value.toInt()
                        deviceIds.forEach { deviceId ->
                            Timber.d("deviceId=$deviceId | colorTemp=$colorTemperature")
                        }
                    }
                    ControlIntent.Color -> {
                        val colorArray = value.split(",")
                        val hue = colorArray[0].toInt()
                        val saturation = colorArray[1].toInt()
                        val brightness = colorArray[2].toInt()
                        deviceIds.forEach { deviceId ->
                            Timber.d(
                                "deviceId=$deviceId | color=hsv($hue, $saturation, $brightness)"
                            )
                        }
                    }
                    else -> {}
                }
                return true
            }

            /**
             * Invoked to query the current devices in user's home
             * @return the list of devices in user's home
             */
            override fun onQueryIotDevice(): List<HomeDevice> {
                val devices = viewModel.getHomeDevices()
                return devices.toList()
            }

            /**
             * Invoked to query the current automation in user's home
             * @return the list of automation in user's home
             */
            override fun onQueryIotAutomations(): List<AutomationInfo> {
                // [STUB]
                val automations = mutableListOf<AutomationInfo>()
                automations.add(AutomationInfo("124356658475478457", "Leave Home"))
                automations.add(AutomationInfo("1095y6658475478485", "Arrive Home"))
                automations.add(AutomationInfo("1095y6658475774539", "Clipping"))
                // [STUB]
                return automations
            }

            /**
             * Invoked to query the current date and time
             * @return the current date and time
             */
            override fun onQueryCurrentDateAndTime(): String {
                val date = getCurrentDateTime()
                Timber.d(date)
                return date
            }

            /**
             * Invoked to query the current home info
             * @return the home info
             */
            override fun onQueryCurrentHomeInfo(): HomeInfo {
                // [STUB]
                val rooms = mutableListOf<RoomInfo>()
                rooms.add(RoomInfo(name = "Attic"))
                rooms.add(RoomInfo(name = "Bathroom"))
                rooms.add(RoomInfo(name = "Veranda"))
                rooms.add(RoomInfo(name = "Living Room"))
                return HomeInfo(
                    name = "Tester's",
                    location = HomeLocation(country = "CN", province = "Zhejiang" , city = "Hangzhou"),
                    rooms = rooms.toList()
                )
                // [STUB]
            }

            /**
             * Invoked to query the current weather info
             *
             * @param cityName the name of the city.
             * @return the weather info
             */
            override fun onQueryCurrentWeather(cityName: String): JSONObject? {
                return null
            }

            /**
             * Invoked to query news
             * @param query the news query
             * @return the news
             */
            override fun onQueryNews(query: String): List<JSONObject>? {
                return null
            }

            /**
             * Invoked to search the web
             * @param query search the web
             * @return the result
             */
            override fun onWebSearch(query: String): String {
                TODO("Not yet implemented")
            }

            /**
             * Invoked to create an automation.
             * @param name a suggested name for the automation.
             * @param loops the days of the week when the automation can be active.
             * @param matchType the logical property for determining when to trigger the automation.
             * @param tasks the list of tasks to be executed by the automation.
             * @param conditions the list of conditions determining when to trigger the automation.
             * @return true when the automation is created successfully, false otherwise.
             */
            override fun onCreateAutomation(
                name: String,
                loops: String,
                matchType: MatchType,
                tasks: MutableList<Task>,
                conditions: MutableList<Condition>
            ): Boolean {
                return true
            }

            /**
             * Invoked to manage an automation
             *
             * @param intent the management intent
             * @param automationIds the automation identifiers
             */
            override fun onManageAutomation(intent: String, automationIds: List<String>): Boolean {
                Timber.d("Intent: $intent")
                Timber.d("Automation Identifiers: $automationIds")
                return true
            }
        }
    }
}
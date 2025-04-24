package com.dsh.openai.home.internal

import android.content.Context
import androidx.lifecycle.*
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.dsh.openai.home.InferenceEngine
import com.dsh.openai.home.InferenceResultListener
import com.dsh.openai.home.InferenceToolsListener
import com.dsh.openai.home.internal.data.ChatMessagesStore
import com.dsh.openai.home.internal.functions.FunctionDefinitions
import com.dsh.openai.home.internal.model.FinishReason
import com.dsh.openai.home.internal.model.FunctionName.*
import com.dsh.openai.home.internal.utils.ArgumentsUtil
import com.dsh.openai.home.internal.utils.extensions.ChatDeltaExt.isEmpty
import com.dsh.openai.home.internal.utils.extensions.FunctionCallExt.isNull
import com.dsh.openai.home.model.OperationFailedException
import com.dsh.openai.home.model.UnsupportedOperationException
import com.dsh.openai.home.model.config.InferenceConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
@OptIn(BetaOpenAI::class)
internal object InferenceEngineImpl : InferenceEngine {

    /**
     * The openai client
     */
    private lateinit var openaiClient: OpenAI

    /**
     * The inference configuration
     */
    private lateinit var config: InferenceConfig

    /**
     * The inference tools listener. Mainly for all the functions calls
     */
    private lateinit var mToolsListener: InferenceToolsListener

    /**
     * The inference result listener. It will propagate all the final responses. Errors included
     */
    private lateinit var mResultListener: InferenceResultListener

    /**
     * The list of chat completion functions
     */
    private val completionFunctions: MutableList<ChatCompletionFunction> = mutableListOf()

    /**
     * The inference history
     */
    private lateinit var messageStore: ChatMessagesStore

    /**
     * The inference job
     */
    private var inferenceJob: CompletableJob = Job()

    /**
     * The inference mutex
     */
    private val inferenceMutex: Mutex = Mutex()

    /**
     * Generates an instance of the engine.
     */
    fun getInstance(
    ): InferenceEngine {
        throw RuntimeException("This interface has been deprecated")
    }

    /**
     * Generates an instance of the engine.
     *
     * @param config the inference config
     * @param toolsListener the inference engine tools listener
     * @param resultListener the inference engine results listener
     */
    fun getInstance(
        @ApplicationContext context: Context,
        config: InferenceConfig,
        toolsListener: InferenceToolsListener,
        resultListener: InferenceResultListener
    ): InferenceEngine {
        // set the inference config
        this.config = config

        // initialize the openai client
        openaiClient = OpenAI(
            token = config.token,
            timeout = Timeout(socket = 60.seconds),
            host = OpenAIHost("${config.baseUrl}/v1/")
        )

        // set the listeners
        this.mToolsListener = toolsListener
        this.mResultListener = resultListener

        // initialize the message repository
        messageStore = ChatMessagesStore(context, config.name)

        // set chat completion functions
        initFunctionDefinitions()

        return this
    }

    /**
     * Sets the chat completion functions
     */
    private fun initFunctionDefinitions() {
        completionFunctions.clear()
        completionFunctions.add(FunctionDefinitions.webSearchFuncDef())
        completionFunctions.add(FunctionDefinitions.queryHomeDevicesFuncDef())
        completionFunctions.add(FunctionDefinitions.deviceControlFuncDef())
        completionFunctions.add(FunctionDefinitions.queryCurrentHomeInfoFuncDef())
        completionFunctions.add(FunctionDefinitions.queryCurrentDateAndTimeFuncDef())
        completionFunctions.add(FunctionDefinitions.queryCurrentWeatherFuncDef())
        completionFunctions.add(FunctionDefinitions.queryNewsFuncDef())
        completionFunctions.add(FunctionDefinitions.createAutomationFuncDef())
        completionFunctions.add(FunctionDefinitions.queryHomeAutomationsFuncDef())
        completionFunctions.add(FunctionDefinitions.automationManagementFuncDef())
    }

    /**
     * Infers the high level language intents.
     * @param intent the intent
     */
    @Deprecated(
        "Use infer(intent: String, stream: Boolean)",
        replaceWith = ReplaceWith("infer(intent: String, stream: Boolean)"),
        level = DeprecationLevel.WARNING
    )
    override suspend fun infer(intent: String) {
        inferenceMutex.withLock {
            // Cancel the previous inference job if it exists
            inferenceJob.cancel()
            // Cancel all child jobs
            inferenceJob.children.forEach { it.cancel() }
            // Mark the job as completed to allow for a new one
            inferenceJob.complete()
            // Cancel the completed job if it is still in the job hierarchy
            inferenceJob.cancel()

            inferenceJob = Job()
            CoroutineScope(Dispatchers.IO + inferenceJob).launch {
                val chatMessage = ChatMessage(
                    role = ChatRole.User,
                    content = intent
                )

                // Copy the chat history
                messageStore.addMessage(chatMessage)

                chatCompletion()
            }
        }
    }

    /**
     * Infers the high level language intents.
     * @param intent the intent
     * @param stream whether to stream the responses or not.
     */
    override suspend fun infer(intent: String, stream: Boolean) {
        inferenceMutex.withLock {
            // Cancel the previous inference job if it exists
            inferenceJob.cancel()
            // Cancel all child jobs
            inferenceJob.children.forEach { it.cancel() }
            // Mark the job as completed to allow for a new one
            inferenceJob.complete()
            // Cancel the completed job if it is still in the job hierarchy
            inferenceJob.cancel()

            inferenceJob = Job()
            CoroutineScope(Dispatchers.IO + inferenceJob).launch {
                val chatMessage = ChatMessage(
                    role = ChatRole.User,
                    content = intent
                )

                // Copy the chat history
                messageStore.addMessage(chatMessage)
                if(stream){
                    chatCompletionStream()
                }else{
                    chatCompletion()
                }
            }
        }
    }

    /**
     * Cancels the ongoing inference process.
     */
    override suspend fun cancel() {
        inferenceMutex.withLock {
            // Cancel the previous inference job if it exists
            inferenceJob.cancel()
            // Cancel all child jobs
            inferenceJob.children.forEach { it.cancel() }
            // Mark the job as completed to allow for a new one
            inferenceJob.complete()
            // Cancel the completed job if it is still in the job hierarchy
            inferenceJob.cancel()
        }
    }

    /**
     * The chat completion function
     */
    private suspend fun chatCompletionStream() {
        // Generate chat completion request
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(this.config.modelId.id),
            functions = completionFunctions,
            functionCall = FunctionMode.Auto,
            messages = messageStore.getMessages(),
        )

        try {
            // Infer intent
            val completionChunkFlow = openaiClient.chatCompletions(chatCompletionRequest)
            handleCompletion(completionChunkFlow)
        }catch (ex: CancellationException){
            val errorMsg = "Failed to infer message. Cause: ${ex.localizedMessage}"
            Timber.e(errorMsg, ex)
        } catch (ex: Exception){
            val errorMsg = "Failed to infer message. Cause: ${ex.localizedMessage}"
            Timber.e(errorMsg, ex)
            mResultListener.onError(RuntimeException(errorMsg, ex))
        }
    }

    /**
     * The chat completion function
     */
    private suspend fun chatCompletion() {
        // Generate chat completion request
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(this.config.modelId.id),
            functions = completionFunctions,
            functionCall = FunctionMode.Auto,
            messages = messageStore.getMessages(),
        )

        try {
            // Infer intent
            val completion: ChatCompletion = openaiClient.chatCompletion(chatCompletionRequest)
            // Process the response from OpenAI
            handleCompletion(completion)
        }catch (ex: CancellationException){
            val errorMsg = "Failed to infer message. Cause: ${ex.localizedMessage}"
            Timber.e(errorMsg, ex)
        } catch (ex: Exception){
            val errorMsg = "Failed to infer message. Cause: ${ex.localizedMessage}"
            Timber.e(errorMsg, ex)
            mResultListener.onError(RuntimeException(errorMsg, ex))
        }
    }

    /**
     * Handles the chat completion response
     *
     * @param completionChunkFlow the chat completion chunk flow
     */
    private suspend fun handleCompletion(completionChunkFlow : Flow<ChatCompletionChunk>) {
        var name = ""
        var chunk = ""
        var arguments = ""
        completionChunkFlow.collect { completionChunk ->
            val choice = completionChunk.choices[0]
            val delta = choice.delta!!
            if(!delta.isEmpty()) {
                if(!delta.content.isNullOrBlank()) {
                    if(chunk.isNotBlank()){
                        mResultListener.onCompletion(
                            data = chunk, stream = true, complete = false
                        )
                    }
                    chunk = delta.content!!
                }else if(!delta.functionCall.isNull()){
                    val functionCall = delta.functionCall!!
                    if(!functionCall.arguments.isNullOrBlank()){
                        arguments += functionCall.arguments!!
                    }

                    if(!functionCall.name.isNullOrBlank()) {
                        name = functionCall.name!!
                    }
                }
            } else if(!choice.finishReason.isNullOrBlank()){
                when(choice.finishReason) {
                    FinishReason.FunctionCall.reason-> {
                        invokeFunction(name, arguments, true)
                    }
                    FinishReason.Stop.reason->{
                        mResultListener.onCompletion(data = chunk, stream = true, complete = true)
                    }
                }
            }
        }
    }

    /**
     * Handles the chat completion response
     *
     * @param completion the chat completion
     */
    private suspend fun handleCompletion(completion: ChatCompletion) {
        val choice = completion.choices[0]
        if(choice.finishReason.isNullOrBlank()) {
            Timber.e("Null or blank finish reason in first choice")
            return
        }

        val chatMessage = choice.message
        when(choice.finishReason!!) {
            FinishReason.FunctionCall.reason-> {
                chatMessage?.let { message ->
                    // Note: We are ignoring function call messages because they have null content
                    val name = message.functionCall?.name
                    val arguments = message.functionCall?.arguments
                    invokeFunction(name, arguments, false)
                }
            }
            FinishReason.Stop.reason->{
                chatMessage?.let { message ->
                    messageStore.addMessage(chatMessage)
                    if(!message.content.isNullOrBlank()) {
                        val content = message.content!!
                        mResultListener.onCompletion(content, stream = false, complete = true)
                    }
                }
            }
        }
    }

    /**
     * Handles device info querying
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleDeviceQuery(stream: Boolean) {
        Timber.d("Querying the device list")
        val content = try {
            val devices = mToolsListener.onQueryIotDevice()
            if(devices.isEmpty())
                "Does not have any devices."
            else
                Gson().toJson(devices)
        }catch (exception: Exception){
            "Something went wrong while processing the request."
        }

        val chatMessage = ChatMessage(
            role = ChatRole.Function,
            name = QueryHomeDevices.name,
            content = content
        )

        // Copy the chat history
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles device info querying
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleAutomationQuery(stream: Boolean) {
        Timber.d("Querying the automation list")
        val content = try {
            val automations = mToolsListener.onQueryIotAutomations()
            if(automations.isEmpty())
                "Does not have any automations."
            else
                Gson().toJson(automations)
        } catch (exception: Exception) {
            "Something went wrong while processing the request."
        }
        val chatMessage = ChatMessage(
            role = ChatRole.Function,
            name = QueryHomeAutomations.name,
            content = content
        )
        // Copy the chat history
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
            chatCompletion()
        }
        }
    }

    /**
     * Handles device control
     * @param arguments function call arguments
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleDeviceControl(arguments: String, stream: Boolean){
        val content = try {
            val controlArgs = ArgumentsUtil.extractDeviceCtrlArgs(arguments)
            Timber.d("Control Args: $controlArgs")
            val status = mToolsListener.onDeviceControl(
                controlArgs.deviceIds,
                controlArgs.intent,
                controlArgs.value
            )
            if(status) "Device control completed successfully" else
                "Failed to control device. Something went wrong"
        }catch (ex: OperationFailedException){
            "Failed to control device. ${ex.localizedMessage}"
        }catch (ex: UnsupportedOperationException) {
            "Failed to control device. ${ex.localizedMessage}"
        } catch (ex: Exception){
            Timber.e("Failed to control device. Cause: ${ex.localizedMessage}")
            "Failed to control device. Something went wrong."
        }

        // Create copy the chat history
        val chatMessage = ChatMessage(
            role = ChatRole.Function,
            name = DeviceControl.name,
            content = content
        )
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles current home data query events
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleCurrentHomeInfoQuery(stream: Boolean){
        Timber.d("Querying the home info")
        val homeInfo = mToolsListener.onQueryCurrentHomeInfo()
        val chatMessage =ChatMessage(
            role = ChatRole.Function,
            name = QueryCurrentHomeInfo.name,
            content = Gson().toJson(homeInfo)
        )

        // Copy the chat history
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles current data and time query events
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleCurrentDateTimeQuery(stream: Boolean){
        Timber.d("Querying the current data and time")
        val dateAndTime = mToolsListener.onQueryCurrentDateAndTime()
        val chatMessage =ChatMessage(
            role = ChatRole.Function,
            name = QueryCurrentDateAndTime.name,
            content = dateAndTime
        )

        // Copy the chat history
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles current weather data query events
     * @param arguments function call arguments
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleWeatherDataQuery(arguments: String, stream: Boolean) {
        val cityName = ArgumentsUtil.extractWeatherQueryArgs(arguments)
        val weather = mToolsListener.onQueryCurrentWeather(cityName)
        val chatMessage =ChatMessage(
            role = ChatRole.Function,
            name = QueryCurrentWeather.name,
            content = Gson().toJson(
                weather ?: "No weather info present at the moment. try again later."
            )
        )

        // Copy the chat history
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles news query events
     * @param arguments function call arguments
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleNewsQuery(arguments: String, stream: Boolean) {
        val query = ArgumentsUtil.extractQueryArgs(arguments)
        val news = mToolsListener.onQueryNews(query)
        val chatMessage = ChatMessage(
            role = ChatRole.Function,
            name = QueryNews.name,
            content = Gson().toJson(news ?: "Failed to fetch news. Try again later.")
        )

        // Copy the chat history
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles web search query events
     * @param arguments function call arguments
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleWebSearch(arguments: String, stream: Boolean) {
        val query = ArgumentsUtil.extractQueryArgs(arguments)
        val result = mToolsListener.onWebSearch(query)
        val chatMessage = ChatMessage(
            role = ChatRole.Function,
            name = WebSearch.name,
            content = result
        )

        // Copy the chat history
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles automation creation events
     * @param arguments function call arguments
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleAutomationCreation(arguments: String, stream: Boolean) {
        val content = try {
            val automationArgs = ArgumentsUtil.extractAutomationArgs(arguments)
            Timber.d("Automation Args: $automationArgs")
            val status = mToolsListener.onCreateAutomation(
                automationArgs.name,
                automationArgs.loops,
                automationArgs.matchType,
                automationArgs.tasks,
                automationArgs.conditions
            )
            if(status) "Automation has been created successfully" else
                "Failed to create automation. Something went wrong"
        }catch (ex: OperationFailedException){
            "Failed to create automation. ${ex.localizedMessage}"
        }catch (ex: UnsupportedOperationException) {
            "Failed to create automation. ${ex.localizedMessage}"
        } catch (ex: Exception){
            Timber.e("Failed to create automation. Cause: ${ex.localizedMessage}")
            "Failed to create automation. Something went wrong"
        }

        // Create copy the chat history
        val chatMessage = ChatMessage(
            role = ChatRole.Function,
            name = CreateAutomation.name,
            content = content
        )
        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Handles the automation management event
     * @param arguments the event arguments
     * @param stream whether to stream the responses or not.
     */
    private suspend fun handleAutomationManagement(arguments: String, stream: Boolean) {
        val content =  try {
            val automationMgtArgs =  ArgumentsUtil.extractAutomationMgtArgs(arguments)
            Timber.d("Automation management Args: $automationMgtArgs")
            val status = mToolsListener.onManageAutomation(
                automationMgtArgs.intent,
                automationMgtArgs.automationIds,
            )
            if(status) "Automation management operation completed successfully" else
                "Automation management operation failed. Something went wrong."
        }catch (ex: OperationFailedException) {
            "Automation management operation failed. ${ex.localizedMessage}"
        }catch (ex: UnsupportedOperationException) {
            "Automation management operation failed. ${ex.localizedMessage}"
        }catch (ex: Exception){
            Timber.e("Automation management failed. Cause: ${ex.localizedMessage}")
            "Automation management operation failed. Something went wrong."
        }

        // Create copy the chat history
        val chatMessage = ChatMessage(
            role = ChatRole.Function,
            name = CreateAutomation.name,
            content = content
        )

        messageStore.addMessage(chatMessage)

        // Complete messages
        when(stream) {
            true -> {
                chatCompletionStream()
            }else -> {
                chatCompletion()
            }
        }
    }

    /**
     * Invokes the functions specified in completion response
     *
     * @param name the name of the function
     * @param arguments the arguments to be passed to the function
     * @param stream whether to stream the responses or not.
     */
    private suspend fun invokeFunction(name: String?, arguments: String?, stream: Boolean) {
        if(name.isNullOrBlank()){
            return
        }

        when(name) {
            QueryHomeDevices.name -> {
                handleDeviceQuery(stream)
            }
            QueryHomeAutomations.name -> {
                handleAutomationQuery(stream)
            }
            DeviceControl.name -> {
                handleDeviceControl(arguments!!, stream)
            }
            QueryCurrentHomeInfo.name -> {
                handleCurrentHomeInfoQuery(stream)
            }
            QueryCurrentDateAndTime.name -> {
                handleCurrentDateTimeQuery(stream)
            }
            QueryCurrentWeather.name -> {
                handleWeatherDataQuery(arguments!!, stream)
            }
            QueryNews.name -> {
                handleNewsQuery(arguments!!, stream)
            }
            WebSearch.name -> {
                handleWebSearch(arguments!!, stream)
            }
            CreateAutomation.name -> {
                handleAutomationCreation(arguments!!, stream)
            }
            ManageAutomation.name -> {
                handleAutomationManagement(arguments!!, stream)
            }
        }
    }

    /**
     * Called when a state transition event happens.
     *
     * @param source The source of the event
     * @param event The event
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Timber.d("Lifecycle Event : $event")
    }
}
package com.dsh.openai.home.internal.data

import android.content.Context
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.dsh.openai.home.internal.utils.InferenceEngineUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(BetaOpenAI::class)
internal class ChatMessagesStore @Inject constructor(
    @ApplicationContext context: Context,
    engineName: String
) {

    /**
     * The inference history
     */
    private val messages: MutableList<ChatMessage> = mutableListOf()

    companion object {
        /**
         * Cache size
         */
        private const val MAX_MESSAGES = 10

        /**
         * The engine name placeholder in prompt
         */
        private const val ENGINE_NAME_KEY = "inf.engine.name"
    }

    init {
        // Set the system message
        val systemMessage = defineSystemRole(context, engineName)
        messages.add(systemMessage)
    }

    /**
     * Generates the system's role message
     *
     * @param context the application context
     * @return the chat message
     */
    private fun defineSystemRole(
        @ApplicationContext context: Context, engineName: String
    ): ChatMessage {
        val prompt = try {
            val data = InferenceEngineUtil.readPrompt(context)
            data.replace(ENGINE_NAME_KEY, engineName)
        }catch (ex: Exception) {
            ChatRole.Assistant.role
        }

        return ChatMessage(
            role = ChatRole.System,
            content = prompt,
        )
    }

    /**
     * Adds a new message to the list
     *
     * @param message the chat message to added
     */
    fun addMessage(message: ChatMessage){
        if(messages.size < MAX_MESSAGES) {
            messages.add(message)
        }else{
            messages.removeAt(1)
            messages.add(message)
        }
    }

    /**
     * Getter for the current list of messages
     *
     * @return the current list of messages
     */
    fun getMessages(): List<ChatMessage> {
        return this.messages.toList()
    }

    /**
     * Clears the current list of messages
     */
    fun clear(){
        this.messages.clear()
    }
}
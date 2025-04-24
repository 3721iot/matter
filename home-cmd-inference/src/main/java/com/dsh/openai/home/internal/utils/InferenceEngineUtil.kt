package com.dsh.openai.home.internal.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream

internal object InferenceEngineUtil {

    /**
     * The file containing the system's role def
     */
    private const val PROMPT_DEF_FILE = "role_definition.txt"

    /**
     * Reads the assistant's role definition
     *
     * @return the assistant's prompt
     */
    fun readPrompt(@ApplicationContext context: Context) : String  {
        val inputStream: InputStream = context.assets.open(PROMPT_DEF_FILE)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        val contents = String(buffer)
        inputStream.close()
        return contents
    }
}
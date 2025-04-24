package com.dsh.openai.home

interface InferenceResultListener {
    /**
     * Invoked on a successful intent inference completion
     * @param data the result data.
     * @param stream whether the result is streamed or not.
     * @param complete whether the inference is completed or not.
     */
    fun onCompletion(data: String, stream: Boolean, complete: Boolean)

    /**
     * Invoked when inference is completed with an error
     * @param exception the exception
     */
    fun onError(exception: Exception)
}
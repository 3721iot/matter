package com.dsh.openai.home.model

data class InferenceResult(
    /**
     * The result data.
     */
    val data: String,

    /**
     * Whether the result is streamed or not.
     */
    val stream: Boolean,

    /**
     * Whether the inference is completed or not.
     */
    val complete: Boolean
)
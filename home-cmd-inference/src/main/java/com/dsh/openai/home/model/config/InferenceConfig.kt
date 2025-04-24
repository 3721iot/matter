package com.dsh.openai.home.model.config

data class InferenceConfig(
    /**
     * The name of the engine
     */
    val name: String,

    /**
     * The OpenAI token
     */
    val token: String,

    /**
     * The OpenAI model
     */
    val modelId: ModelIdentifier,

    /**
     * The OpenAI base url
     */
    val baseUrl: String,
)

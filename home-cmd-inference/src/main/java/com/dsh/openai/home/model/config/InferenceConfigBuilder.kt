package com.dsh.openai.home.model.config

class InferenceConfigBuilder {
    /**
     * The name of the engine
     */
    private var name: String? = null

    /**
     * The OpenAI token
     */
    private var token: String? = null

    /**
     * The OpenAI model
     */
    private var modelId: ModelIdentifier? = null

    /**
     * The OpenAI base url
     */
    private var baseUrl: String? = null

    /**
     * Setter for the engine name
     *
     * @param name the name of the engine.
     * @return an instance of the [InferenceConfigBuilder].
     */
    fun setName(name: String): InferenceConfigBuilder {
        this.name = name
        return this
    }

    /**
     * Setter for the OpenAI token
     *
     * @param token the OpenAI api token
     * @return an instance of the [InferenceConfigBuilder].
     */
    fun setToken(token: String): InferenceConfigBuilder {
        this.token = token
        return this
    }

    /**
     * Setter for the model identifier
     *
     * @param modelId the model identifier
     * @return an instance of the [InferenceConfigBuilder].
     */
    fun setModelId(modelId: ModelIdentifier): InferenceConfigBuilder {
        this.modelId = modelId
        return this
    }

    /**
     * Setter for the OpenAI api base url.
     *
     * @param baseUrl the OpenAI api base url.
     * @return an instance of the [InferenceConfigBuilder].
     */
    fun setBaseUrl(baseUrl: String): InferenceConfigBuilder {
        this.baseUrl = baseUrl
        return this
    }

    /**
     *  Builds the inference configuration
     *  @return the inference config
     */
    fun build(): InferenceConfig {
        if(null == this.token || null == this.baseUrl){
            throw IllegalArgumentException("The token or base url cannot be null")
        }

        return InferenceConfig(
            token = this.token!!,
            baseUrl = this.baseUrl!!,
            modelId = if (null == this.modelId) ModelIdentifier.ThreePointFive16k else this.modelId!!,
            name = if(null == this.name) "Duchess" else this.name!!
        )
    }
}
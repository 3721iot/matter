package com.dsh.openai.home.internal.functions

import com.aallam.openai.api.chat.Parameters
import org.json.JSONArray
import org.json.JSONObject

class FunctionParamsBuilder {

    /**
     * The parameter type
     */
    private var type: String? = null

    /**
     * The parameter's required properties
     */
    private var required: JSONArray = JSONArray()

    /**
     * The parameter properties
     */
    private var properties: JSONObject = JSONObject()

    /**
     * The the type setter
     */
    fun setType(type: String): FunctionParamsBuilder {
        this.type = type
        return this
    }

    /**
     * The properties setter
     *
     * @param properties the properties
     * @return an instance of the FunctionParamsBuilder
     */
    fun setProperties(properties: JSONObject): FunctionParamsBuilder {
        this.properties = properties
        return this
    }

    /**
     * The required properties setter
     *
     * @param required the required fields
     * @return an instance of the FunctionParamsBuilder
     */
    fun setRequired(required: JSONArray): FunctionParamsBuilder {
        this.required = required
        return this
    }

    /**
     * The builder
     *
     * @return the function parameters
     */
    fun build() : Parameters {
        val parameters = JSONObject()
        parameters.put(TYPE_KEY, this.type)
        parameters.put(PROPERTIES_KEY, this.properties)
        parameters.put(REQUIRED_KEY, this.required)
        return Parameters.fromJsonString(parameters.toString())
    }

    companion object {

        /**
         * The type field key
         */
        private const val TYPE_KEY = "type"

        /**
         * The required field key
         */
        private const val REQUIRED_KEY = "required"

        /**
         * The properties field key
         */
        private const val PROPERTIES_KEY = "properties"
    }
}

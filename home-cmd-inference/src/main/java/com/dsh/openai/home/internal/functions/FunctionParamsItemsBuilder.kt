package com.dsh.openai.home.internal.functions

import org.json.JSONArray
import org.json.JSONObject

class FunctionParamsItemsBuilder {
    /**
     * The type
     */
    private var type: String? = null

    /**
     * The required properties
     */
    private var required: JSONArray = JSONArray()

    /**
     * The properties
     */
    private var properties: JSONObject = JSONObject()

    /**
     * The description
     */
    private var  description: String? = null

    /**
     * The the type setter
     */
    fun setType(type: String): FunctionParamsItemsBuilder {
        this.type = type
        return this
    }

    /**
     * The the description setter
     */
    fun setDescription(description: String): FunctionParamsItemsBuilder {
        this.description = description
        return this
    }

    /**
     * The properties setter
     *
     * @param properties the properties
     * @return an instance of the FunctionParamsItemsBuilder
     */
    fun setProperties(properties: JSONObject): FunctionParamsItemsBuilder {
        this.properties = properties
        return this
    }

    /**
     * The required properties setter
     *
     * @param required the required fields
     * @return an instance of the FunctionParamsItemsBuilder
     */
    fun setRequired(required: JSONArray): FunctionParamsItemsBuilder {
        this.required = required
        return this
    }

    /**
     * The builder
     *
     * @return the items
     */
    fun build() : JSONObject {
        val items = JSONObject()
        items.put(TYPE_KEY, this.type)
        items.put(PROPERTIES_KEY, this.properties)
        items.put(REQUIRED_KEY, this.required)
        items.put(DESCRIPTION_KEY, this.description)
        return items
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

        /**
         * The description field key
         */
        private const val DESCRIPTION_KEY= "description"
    }
}
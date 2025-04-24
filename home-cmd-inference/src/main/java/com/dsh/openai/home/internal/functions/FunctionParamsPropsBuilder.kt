package com.dsh.openai.home.internal.functions

import org.json.JSONArray
import org.json.JSONObject
import com.dsh.openai.home.internal.model.device.ControlProperty

class FunctionParamsPropsBuilder {

    /**
     * The parameter properties
     */
    private val properties = JSONObject()

    /**
     * Appends all arguments to the given FunctionParamsPropsBuilder.
     *
     * @return an instance of the FunctionParamsPropsBuilder
     */
    fun append(
        name: String, type: String, description: String
    ) : FunctionParamsPropsBuilder {
        // build property
        val property = JSONObject()
        property.put(TYPE_KEY, type)
        property.put(DESCRIPTION_KEY,description)

        // Add property
        properties.put(name, property)
        return this
    }

    /**
     * Appends all arguments to the given FunctionParamsPropsBuilder.
     *
     * @return an instance of the FunctionParamsPropsBuilder
     */
    fun append(
        name: String, type: String, enum: JSONArray
    ) : FunctionParamsPropsBuilder {
        // build property
        val property = JSONObject()
        property.put(TYPE_KEY, type)
        property.put(ENUM_KEY, enum)

        // Add property
        properties.put(name, property)
        return this
    }

    /**
     * Appends all arguments to the given FunctionParamsPropsBuilder.
     *
     * @return an instance of the FunctionParamsPropsBuilder
     */
    fun append(
        name: String, type: String, description: String, enum: JSONArray
    ) : FunctionParamsPropsBuilder {
        // build property
        val property = JSONObject()
        property.put(TYPE_KEY, type)
        property.put(ENUM_KEY, enum)
        property.put(DESCRIPTION_KEY,description)

        // Add property
        properties.put(name, property)
        return this
    }

    /**
     * Appends all arguments to the given FunctionParamsPropsBuilder.
     *
     * @return an instance of the FunctionParamsPropsBuilder
     */
    fun append(
        name: String, type: String, description: String, items: JSONObject
    ) : FunctionParamsPropsBuilder {
        // build property
        val property = JSONObject()
        property.put(TYPE_KEY, type)
        property.put(DESCRIPTION_KEY, description)
        property.put(ControlProperty.Items.property, items)

        // Add property
        properties.put(name, property)
        return this
    }

    /**
     * Builds the function param properties
     *
     * @return the function properties params
     */
    fun build(): JSONObject {
        return this.properties
    }

    companion object {

        /**
         * The type field key
         */
        private const val TYPE_KEY = "type"

        /**
         * The enum field key
         */
        private const val ENUM_KEY = "enum"

        /**
         * The description field key
         */
        private const val DESCRIPTION_KEY = "description"
    }
}
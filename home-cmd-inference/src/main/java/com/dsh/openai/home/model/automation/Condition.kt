package com.dsh.openai.home.model.automation
data class Condition(
    /**
     * An expression used for condition value comparison against realtime values.
     * @see [Expression] for types of comparisons.
     */
    val expression: String,

    /**
     * The name of the condition property.
     * @see [PropertyName] for a complete list of properties.
     */
    val propertyName: String,

    /**
     * The units for a certain property.
     */
    val units: String,

    /**
     * The value.
     */
    val value: String,
)
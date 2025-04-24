package com.dsh.openai.home.model

data class HomeLocation(
    /**
     * The country
     */
    val country: String,

    /**
     * The province
     */
    val province: String,

    /**
     * The city
     */
    val city: String
)

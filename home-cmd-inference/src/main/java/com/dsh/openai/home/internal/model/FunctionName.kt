package com.dsh.openai.home.internal.model

enum class FunctionName {
    /**
     * Queries the home info for the current home
     */
    QueryCurrentHomeInfo,

    /**
     * Queries the home devices
     */
    QueryHomeDevices,

    /**
     * Queries the home automations
     */
    QueryHomeAutomations,

    /**
     * Queries the current date and time
     */
    QueryCurrentDateAndTime,

    /**
     * Queries the current weather
     */
    QueryCurrentWeather,

    /**
     * Queries the news
     */
    QueryNews,

    /**
     * Searches the Web
     */
    WebSearch,

    /**
     * Controls the devices
     */
    DeviceControl,

    /**
     * Creates an automation
     */
    CreateAutomation,

    /**
     * Manages automations
     */
    ManageAutomation
}
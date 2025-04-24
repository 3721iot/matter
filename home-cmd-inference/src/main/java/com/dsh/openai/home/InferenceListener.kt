package com.dsh.openai.home

import com.dsh.openai.home.model.HomeDevice
import com.dsh.openai.home.model.HomeInfo
import com.dsh.openai.home.model.automation.AutomationInfo
import com.dsh.openai.home.model.automation.Condition
import com.dsh.openai.home.model.automation.MatchType
import com.dsh.openai.home.model.automation.Task
import com.dsh.openai.home.model.ControlIntent
import org.json.JSONObject

interface InferenceListener {

    /**
     * Invoked to pass the device control arguments
     *
     * @param deviceIds the list of device identifiers
     * @param intent the device control intent
     * @param value the device control value
     */
    fun onDeviceControl(deviceIds: List<String>, intent: ControlIntent, value: String): Boolean

    /**
     * Invoked to query the current devices in user's home
     * @return the list of devices in user's home
     */
    fun onQueryIotDevice(): List<HomeDevice>

    /**
     * Invoked to query the current automation in user's home
     * @return the list of automation in user's home
     */
    fun onQueryIotAutomations(): List<AutomationInfo>

    /**
     * Invoked to query the current date and time
     * @return the current date and time
     */
    fun onQueryCurrentDateAndTime(): String

    /**
     * Invoked to query the current home info
     * @return the home info
     */
    fun onQueryCurrentHomeInfo(): HomeInfo

    /**
     * Invoked to query the current weather info
     * @param cityName the name of the city
     * @return the weather info
     */
    fun onQueryCurrentWeather(cityName: String): JSONObject ?

    /**
     * Invoked to query news
     * @param query the news query
     * @return the news
     */
    fun onQueryNews(query: String): List<JSONObject>?

    /**
     * Invoked to search the web
     * @param query search the web
     * @return the result
     */
    fun onWebSearch(query: String): String

    /**
     * Invoked to create an automation.
     * @param name a suggested name for the automation.
     * @param loops the days of the week when the automation can be active.
     * @param matchType the logical property for determining when to trigger the automation.
     * @param tasks the list of tasks to be executed by the automation.
     * @param conditions the list of conditions determining when to trigger the automation.
     * @return true when the automation is created successfully, false otherwise.
     */
    fun onCreateAutomation(
        name: String,
        loops: String,
        matchType: MatchType,
        tasks: MutableList<Task>,
        conditions: MutableList<Condition>
    ): Boolean

    /**
     * Invoked to manage an automation
     *
     * @param intent the management intent
     * @param automationIds the automation identifiers
     */
    fun onManageAutomation(intent: String, automationIds: List<String>): Boolean

    /**
     * Invoked on a successful intent inference completion
     * @param result the inference result
     */
    fun onCompletion(result: String)

    /**
     * Invoked when inference is completed with an error
     * @param exception the exception
     */
    fun onError(exception: Exception)
}
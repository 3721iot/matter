package com.dsh.openai.home.internal.utils

import com.dsh.openai.home.internal.model.device.AutomationArgs
import com.dsh.openai.home.internal.model.device.AutomationMgtArgs
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.AutomationIds
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.CityName
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Conditions
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.DeviceIds
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Intent
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Loops
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.MatchType
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Name
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Query
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Tasks
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Value
import com.dsh.openai.home.internal.model.device.DeviceControlsArgs
import com.dsh.openai.home.model.ControlIntent
import com.dsh.openai.home.model.ControlIntent.Brightness
import com.dsh.openai.home.model.ControlIntent.Color
import com.dsh.openai.home.model.ControlIntent.ColorTemperature
import com.dsh.openai.home.model.ControlIntent.Power
import com.dsh.openai.home.model.ControlIntent.Undefined
import com.dsh.openai.home.model.automation.Condition
import com.dsh.openai.home.model.automation.MatchType
import com.dsh.openai.home.model.automation.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

object ArgumentsUtil {

    /**
     * Converts the control intent to enum type
     * @param intent the control intent
     * @return the enum typed control intent
     */
    private fun toControlIntent(intent: String): ControlIntent {
        return when(intent){
            Color.intent->{
                Color
            }
            Brightness.intent->{
                Brightness
            }
            ColorTemperature.intent->{
                ColorTemperature
            }
            Power.intent->{
                Power
            }
            else->{
                Undefined
            }
        }
    }

    /**
     * Converts arguments to formatted device control control args
     * @param arguments the args json string
     * @return device control arguments
     */
    fun extractDeviceCtrlArgs(arguments: String): DeviceControlsArgs {
        val jsonArgs = JSONObject(arguments)
        val deviceIdsJson = jsonArgs.get(DeviceIds.property)
        val deviceIdType = object : TypeToken<MutableList<String>>() {}.type
        val deviceIds = Gson().fromJson<MutableList<String>>(deviceIdsJson.toString(), deviceIdType)
        val intent = toControlIntent(jsonArgs.get(Intent.property).toString())
        val value =  jsonArgs.get(Value.property).toString()
        return DeviceControlsArgs(deviceIds, intent, value)
    }

    /**
     * Converts arguments to formatted current weather query args
     * @param arguments the args json string
     * @return the city name
     */
    fun extractWeatherQueryArgs(arguments: String): String {
        val jsonArgs = JSONObject(arguments)
        val cityName =  jsonArgs.get(CityName.property).toString()
        return cityName.ifBlank {
            ""
        }
    }

    /**
     * Converts arguments to formatted news query args
     * @param arguments the args json string
     * @return the city name
     */
    fun extractQueryArgs(arguments: String): String {
        val jsonArgs = JSONObject(arguments)
        val newsQuery =  jsonArgs.get(Query.property).toString()
        return newsQuery.ifBlank {
            ""
        }
    }

    /**
     * Converts arguments to automation args
     * @param arguments the args json string
     * @return the automation args
     */
    fun extractAutomationArgs(arguments: String): AutomationArgs {
        val jsonArgs = JSONObject(arguments)

        // extract tasks
        val tasksJson = jsonArgs.getJSONArray(Tasks.property)
        val tasksType = object : TypeToken<MutableList<Task>>() {}.type
        val tasks = Gson().fromJson<MutableList<Task>>(tasksJson.toString(), tasksType)

        // extract conditions
        val conditionsJson = jsonArgs.getJSONArray(Conditions.property)
        val conditionsType = object : TypeToken<MutableList<Condition>>() {}.type
        val conditions = Gson().fromJson<MutableList<Condition>>(
            conditionsJson.toString(), conditionsType
        )

        // extract name, loops, and match type
        val name = jsonArgs.get(Name.property).toString()
        val loops = jsonArgs.get(Loops.property).toString()
        val matchType = jsonArgs.get(MatchType.property).toString()

        // build the automation
        return AutomationArgs(name, loops, MatchType(matchType), tasks, conditions)
    }

    /**
     * Converts arguments to automation management args
     *
     * @param arguments the args json string
     * @return the automation managements args
     */
    fun extractAutomationMgtArgs(arguments: String): AutomationMgtArgs{
        val jsonArgs = JSONObject(arguments)

        // extract automation identifiers
        val automationIdsJson = jsonArgs.get(AutomationIds.property)
        val automationIdType = object : TypeToken<MutableList<String>>() {}.type
        val automationIds = Gson().fromJson<MutableList<String>>(
            automationIdsJson.toString(), automationIdType
        )

        // extract the automation management intent
        val intent = jsonArgs.get(Intent.property).toString()
        return AutomationMgtArgs(intent, automationIds)
    }
}
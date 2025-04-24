package com.dsh.openai.home.internal.functions

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionFunction
import com.dsh.openai.home.internal.model.FunctionName.*
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.AutomationIds
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.CityName
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Conditions
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.DeviceIds
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Expression
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Intent
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Loops
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.MatchType
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Name
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.PropertyName
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Query
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.TargetId
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Tasks
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Units
import com.dsh.openai.home.internal.model.device.ControlProperty.Companion.Value
import com.dsh.openai.home.model.automation.Expression.Companion.Equal
import com.dsh.openai.home.model.automation.Expression.Companion.GreaterThan
import com.dsh.openai.home.model.automation.Expression.Companion.GreaterThanOrEqual
import com.dsh.openai.home.model.automation.Expression.Companion.LessThan
import com.dsh.openai.home.model.automation.Expression.Companion.LessThanOrEqual
import com.dsh.openai.home.model.automation.MatchType.Companion.All
import com.dsh.openai.home.model.automation.MatchType.Companion.Any
import com.dsh.openai.home.model.automation.PropertyName.Companion.AirQuality
import com.dsh.openai.home.model.automation.PropertyName.Companion.DeviceStatusChange
import com.dsh.openai.home.model.automation.PropertyName.Companion.Humidity
import com.dsh.openai.home.model.automation.PropertyName.Companion.Schedule
import com.dsh.openai.home.model.automation.PropertyName.Companion.SunsetSunrise
import com.dsh.openai.home.model.automation.PropertyName.Companion.Temperature
import com.dsh.openai.home.model.automation.PropertyName.Companion.Weather
import com.dsh.openai.home.model.automation.PropertyName.Companion.WindSpeed
import com.dsh.openai.home.model.ControlIntent.Brightness
import com.dsh.openai.home.model.ControlIntent.Color
import com.dsh.openai.home.model.ControlIntent.ColorTemperature
import com.dsh.openai.home.model.ControlIntent.Power
import com.dsh.openai.home.model.automation.ManagementIntent.Delete
import com.dsh.openai.home.model.automation.ManagementIntent.Trigger
import com.dsh.openai.home.model.automation.ManagementIntent.Enable
import com.dsh.openai.home.model.automation.ManagementIntent.Disable
import org.json.JSONArray
import org.json.JSONObject

@OptIn(BetaOpenAI::class)
object FunctionDefinitions {

    /**
     * Data types
     */
    private const val ARRAY_TYPE = "ARRAY"
    private const val OBJECT_TYPE = "OBJECT"
    private const val STRING_TYPE = "STRING"

    /**
     * The def for querying home devices
     *
     * @return the chat completion function definition
     */
    fun queryHomeDevicesFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Returns a list of devices that can be changed in state.")
            .append("This function must be called before all the other functions").toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                DeviceIds.property,
                ARRAY_TYPE.lowercase(),
                "Comma-separated list of device identifiers",
                JSONObject().put("type", "string")
            )
            .build()

        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(QueryHomeDevices.name, description, parameters)
    }

    /**
     * The def for querying home automations
     *
     * @return the chat completion function definition
     */
    fun queryHomeAutomationsFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Returns a list of automations. Call this function whenever there is need ")
            .append("to know the automation info").toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                AutomationIds.property,
                ARRAY_TYPE.lowercase(),
                "list of automation identifiers",
                JSONObject().put("type", "string")
            )
            .build()

        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(QueryHomeAutomations.name, description, parameters)
    }

    /**
     * The def for the current home info function
     *
     * @return the chat completion function
     */
    fun queryCurrentHomeInfoFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Returns the info about the current home and the user's location")
            .toString()

        val properties = FunctionParamsPropsBuilder().build()

        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(QueryCurrentHomeInfo.name, description, parameters)
    }

    /**
     * The def for the current date and time function
     *
     * @return the chat completion function
     */
    fun queryCurrentDateAndTimeFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Returns the current date and time")
            .toString()

        val properties = FunctionParamsPropsBuilder().build()
        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(QueryCurrentDateAndTime.name, description, parameters)
    }

    /**
     * The def for the current weather function
     *
     * @return the chat completion function
     */
    fun queryCurrentWeatherFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Get the current weather in a given city. ")
            .append("You can get the city from location given in home info")
            .toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                CityName.property,
                STRING_TYPE.lowercase(),
                "The city name, e.g. San Francisco or Hangzhou"
            )
            .build()

        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setRequired(JSONArray().put(CityName.property))
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(QueryCurrentWeather.name, description, parameters)
    }

    /**
     * The def for the news function
     *
     * @return the chat completion function
     */
    fun queryNewsFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Get the current news headlines. ")
            .append("From this function's response, only use the titles and the bodies to ")
            .append("generate a news article. Refrain from including any href, links, numbering, ")
            .append("or bullets in the final output. The article should be a paragraphed, ")
            .append("continuous piece that is readable. It should sound like a report from a ")
            .append("news desk. Whenever new sources are provided, cleverly include their names. ")
            .append("Additionally, at the end tell the user to check those sources for more info.")
            .toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                Query.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The query from the user. eg if a user inputs top stories, ")
                    .append("a valid query should be what are the top stories for today").toString()
            ).build()
        val parameters =  FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setRequired(JSONArray().put(Query.property))
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(QueryNews.name, description, parameters)
    }

    /**
     * The def for the web search function
     *
     * @return the chat completion function
     */
    fun webSearchFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Get the real-time information about specific events")
            .append("This function will provide you with access to real-time information ")
            .append("about specific events. When a user ask you something related to current events")
            .append(" or anything that the other functions can not handle, ")
            .append("use this function to get more information.")
            .toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                Query.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The query from the user. You should call this function")
                    .append("with exactly what the user said.").toString()
            ).build()
        val parameters =  FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setRequired(JSONArray().put(Query.property))
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(WebSearch.name, description, parameters)
    }

    /**
     * The def for device control function
     *
     * @return the chat completion function
     */
    fun deviceControlFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Use this to control devices. For color control, the ")
            .append("color data MUST be an HSV comma-separated list, with hue, saturation, and")
            .append("value as strings. For color-temperature, the values MUST be in kelvins ")
            .append("(2700K to 6500K). For brightness, the values MUST be a percentage. ")
            .append("Call QueryHomeDevices first to retrieve available devices.")
            .toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                DeviceIds.property,
                ARRAY_TYPE.lowercase(),
                StringBuilder().append("List of device identifiers QueryHomeDevices function. This ")
                    .append("list should always contain device identifiers ONLY")
                    .toString(),
                JSONObject().put("type", "string")
            )
            .append(
                Intent.property,
                STRING_TYPE.lowercase(),
                "The type of device control to be performed on the list of devices",
                JSONArray().put(Power.intent).put(Color.intent)
                    .put(Brightness.intent).put(ColorTemperature.intent)
            )
            .append(
                Value.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The device control value, for example, if the ")
                    .append("'intent'='power', the value should be a boolean.").toString()
            )
            .build()

        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setRequired(
                JSONArray().put(DeviceIds.property).put(Intent.property).put(Value.property)
            )
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(DeviceControl.name, description, parameters)
    }

    /**
     * The automation tasks items definition
     *
     * @return the tasks items definition
     */
    private fun getAutomationTasksItemsDef(): JSONObject {
        // build task properties
        val taskProperties = FunctionParamsPropsBuilder()
            .append(
                TargetId.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The identifier of the target entity. This should be ")
                    .append("a valid identifier from the data you were provided with.")
                    .append("Most of the times this is a device identifier.").toString()
            ).append(
                Intent.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The identifier of the target entity. This should be ")
                    .append("a valid identifier from the data you were provided with.")
                    .append("Most of the times this is a device identifier.").toString(),
                JSONArray().put(Power.intent).put(Brightness.intent)
                    .put(ColorTemperature.intent).put(Color.intent)
            ).append(
                Value.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The value for the action to be performed. for example, ")
                    .append("if the action='power' value='true' or 'false' ").toString()
            ).build()

        // build the item
        return FunctionParamsItemsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setDescription(StringBuilder().append("Each entity should have it's own item data. ")
                .append("If there is more than one device NEVER use value like 'all_devices', ")
                .append("'lights'. Create a 'tasks' entity for each device separately. ")
                .append("Call QueryHomeDevices first to retrieve available devices.").toString())
            .setRequired(JSONArray().put(TargetId.property).put(Intent.property).put(Value.property))
            .setProperties(taskProperties)
            .build()
    }

    /**
     * The automation condition items definition
     *
     * @return the condition items definition
     */
    private fun getAutomationConditionsItemsDef(): JSONObject {
        // build items properties
        val conditionProperties = FunctionParamsPropsBuilder()
            .append(
                Expression.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The condition expression that needs to be satisfied ")
                    .append("for the automation to trigger.").toString(),
                JSONArray().put(Equal.sign).put(GreaterThan.sign)
                    .put(LessThan.sign).put(GreaterThanOrEqual.sign).put(LessThanOrEqual.sign)
            ).append(
                PropertyName.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The property name for the condition with ")
                    .append("the provided expression").toString(),
                JSONArray().put(Schedule.name).put(Temperature.name).put(Humidity.name)
                    .put(WindSpeed.name).put(AirQuality.name).put(SunsetSunrise.name)
                    .put(Weather.name).put(DeviceStatusChange.name)
            ).append(
                Units.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The units for the property name. ")
                    .append("These could be '°C' or 'K' for temperature, '%' for humidity, etc.")
                    .append("Note that this can be 'none' for properties without units").toString(),
                JSONArray().put("°C").put("m/s").put("weather-condition")
                    .put("time").put("none")
            ).append(
                Value.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("The value for the property.  For example, if the ")
                    .append("propertyName='schedule' the value could be '22:00' and maintain ")
                    .append("24hr format. For weather-condition, use the values ")
                    .append("['cloudy','sunny','rainy','snowy','hazy']. For humidity, use the ")
                    .append("values ['high','medium','low']. For air-quality, use the ")
                    .append("values ['excellent','fair','poor'].")
                    .toString()
            )
            .build()

        // build the condition properties
        return FunctionParamsItemsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setRequired(JSONArray().put(Expression.property)
                .put(PropertyName.property).put(Value.property)
                .put(Units.property))
            .setProperties(conditionProperties)
            .build()
    }

    /**
     * The def for automation creation function
     *
     * @return the chat completion function
     */
    fun createAutomationFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Use this function to create a smart home automation based on the provided ")
            .append("details. If there are no conditions specified for the automation, ")
            .append("pass an empty list for the value of 'conditions'. Call QueryHomeDevices ")
            .append("first to retrieve available devices. For attributes with values ")
            .append("provided in the descriptions, ALWAYS use the values from the supported values")
            .toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                Tasks.property,
                ARRAY_TYPE.lowercase(),
                StringBuilder().append("A list of tasks data objects describing the ")
                    .append("target devices and control tasks.").toString(),
                getAutomationTasksItemsDef()
            )
            .append(
                Conditions.property,
                ARRAY_TYPE.lowercase(),
                StringBuilder().append("A list of condition data objects describing the ")
                    .append("conditions for when the automation should trigger. This can be an empty ")
                    .append("list if there are no conditions provided").toString(),
                getAutomationConditionsItemsDef()
            )
            .append(
                Loops.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("A binary representation of the days of the week when ")
                    .append("the automation should be active. The week starts on Sunday. ")
                    .append("For example, loops='1000001' means active on Sunday and Saturday only.")
                    .toString()
            )
            .append(
                MatchType.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("This determines whether to trigger the automation when ")
                    .append("all the conditions are met or just when any condition is met.")
                    .toString(),
                JSONArray().put(All.type).put(Any.type)
            )
            .append(
                Name.property,
                STRING_TYPE.lowercase(),
                StringBuilder().append("A suggestion for the automation name.").toString()
            )
            .build()

        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setRequired(JSONArray().put(Name.property).put(Tasks.property)
                .put(Conditions.property).put(Loops.property).put(MatchType.property))
            .setProperties(properties)
            .build()
        return ChatCompletionFunction(CreateAutomation.name, description, parameters)
    }

    /**
     * The def for automation management function
     *
     * @return the chat completion function
     */
    fun automationManagementFuncDef(): ChatCompletionFunction {
        val description = StringBuilder()
            .append("Use this to start, set, trigger, delete, enable and disable automations.")
            .append("Call QueryHomeAutomations first to retrieve available automations.")
            .toString()

        val properties = FunctionParamsPropsBuilder()
            .append(
                AutomationIds.property,
                ARRAY_TYPE.lowercase(),
                StringBuilder().append("List of device identifiers QueryHomeAutomations function. This ")
                    .append("list should always contain automation identifiers ONLY")
                    .toString(),
                JSONObject().put("type", "string")
            )
            .append(
                Intent.property,
                STRING_TYPE.lowercase(),
                "The type of automation management operation to be performed on the list of automation",
                JSONArray().put(Delete.intent).put(Trigger.intent)
                    .put(Enable.intent).put(Disable.intent)
            )
            .build()

        val parameters = FunctionParamsBuilder()
            .setType(OBJECT_TYPE.lowercase())
            .setRequired(
                JSONArray().put(AutomationIds.property).put(Intent.property)
            )
            .setProperties(properties)
            .build()

        return ChatCompletionFunction(ManageAutomation.name, description, parameters)
    }
}
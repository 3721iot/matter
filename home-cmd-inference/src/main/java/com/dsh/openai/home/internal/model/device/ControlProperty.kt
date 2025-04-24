package com.dsh.openai.home.internal.model.device

@JvmInline
value class ControlProperty(val property: String) {
    companion object {
        @JvmStatic
        val Value: ControlProperty = ControlProperty("value")
        @JvmStatic
        val DeviceIds: ControlProperty = ControlProperty("deviceIds")
        @JvmStatic
        val CityName: ControlProperty = ControlProperty("cityName")
        @JvmStatic
        val Query: ControlProperty = ControlProperty("query")
        @JvmStatic
        val Intent: ControlProperty = ControlProperty("controlIntent")
        @JvmStatic
        val Tasks: ControlProperty = ControlProperty("tasks")
        @JvmStatic
        val Conditions: ControlProperty = ControlProperty("conditions")
        @JvmStatic
        val PropertyName: ControlProperty = ControlProperty("propertyName")
        @JvmStatic
        val Units: ControlProperty = ControlProperty("units")
        @JvmStatic
        val Expression: ControlProperty = ControlProperty("expression")
        @JvmStatic
        val TargetId: ControlProperty = ControlProperty("targetId")
        @JvmStatic
        val Name: ControlProperty = ControlProperty("name")
        @JvmStatic
        val Items: ControlProperty = ControlProperty("items")
        @JvmStatic
        val Loops: ControlProperty = ControlProperty("loops")
        @JvmStatic
        val MatchType: ControlProperty = ControlProperty("matchType")
        @JvmStatic
        val AutomationIds: ControlProperty = ControlProperty("automationIds")
    }
}

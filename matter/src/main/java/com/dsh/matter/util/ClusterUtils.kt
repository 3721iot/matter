package com.dsh.matter.util

import chip.devicecontroller.ChipClusters.*
import chip.devicecontroller.model.ClusterState
import chip.devicecontroller.model.EndpointState
import chip.devicecontroller.model.NodeState
import com.dsh.matter.model.cluster.SubscriptionClusters
import com.dsh.matter.model.color.HSVColor
import com.dsh.matter.model.device.FanMode
import com.dsh.matter.model.device.StateAttribute
import com.dsh.matter.util.device.FanModeUtil
import timber.log.Timber

internal object ClusterUtils {

    /**
     * Attributes identifiers
     */
    private const val HUE_ATTRIBUTE_ID = 0L
    private const val ON_OFF_ATTRIBUTE_ID = 0L
    private const val MIREDS_ATTRIBUTE_ID = 7L
    private const val SATURATION_ATTRIBUTE_ID = 1L
    private const val CURRENT_LEVEL_ATTRIBUTE_ID = 0L
    private const val FAN_MODE_ATTRIBUTE_ID = 0L
    private const val PERCENTAGE_SETTING_ATTRIBUTE_ID = 2L

    /**
     * Extract device's cluster attribute value from [NodeState]
     *
     * @param nodeState node/device state
     */
    fun handleNodeStateReport(nodeState: NodeState?): HashMap<StateAttribute, Any> {
        val states = HashMap<StateAttribute, Any>()
        states[StateAttribute.Online] = true
        if(null == nodeState){
            return states
        }

        // process node states
        nodeState.endpointStates.forEach{ (_endpointId, endpointState) ->
            if(_endpointId != 1){
                return@forEach
            }

            try {
                val endpointStates = extractEndpointStates(endpointState)
                states.putAll(endpointStates)
            }catch (ex: Exception) {
                Timber.e("Failed to extract endpoint states")
            }

        }
        return states
    }

    /**
     * Extracts the endpoint states
     *
     * @param endpointState endpoint
     */
    private fun extractEndpointStates(endpointState: EndpointState): HashMap<StateAttribute, Any> {
        val clusterStates = HashMap<StateAttribute, Any>()
        endpointState.clusterStates.forEach{(_clusterId, clusterState) ->
            if (!SubscriptionClusters.map.contains(_clusterId)) {
                return@forEach
            }

            when(_clusterId) {
                OnOffCluster.CLUSTER_ID -> {
                    val on = extractOnOffAttributeValue(clusterState)
                    if(null != on) {
                        clusterStates[StateAttribute.Switch] = on
                    }
                }
                LevelControlCluster.CLUSTER_ID -> {
                    val level = extractLevelControlAttributeValue(clusterState)
                    if(null != level) {
                        clusterStates[StateAttribute.Brightness] = level
                    }
                }
                ColorControlCluster.CLUSTER_ID -> {
                    val color = extractColorControlAttributeValue(clusterState)
                    if(null != color) {
                        clusterStates[StateAttribute.Color] = color
                    }

                    val temperature = extractColorControlMiredsAttributeValue(clusterState)
                    if(null != temperature){
                        clusterStates[StateAttribute.ColorTemperature] = temperature
                    }
                }
                FanControlCluster.CLUSTER_ID -> {
                    val fanMode = extractFanControlFanModeAttributeValue(clusterState)
                    if(null != fanMode) {
                        clusterStates[StateAttribute.FanMode] = fanMode
                        clusterStates[StateAttribute.Switch] = (fanMode != FanMode.Off)
                    }
                    val fanSpeed = extractFanControlPercentageSettingAttributeValue(clusterState)
                    if(null != fanSpeed) {
                        clusterStates[StateAttribute.FanSpeed] = fanSpeed
                    }
                }
                else -> {}
            }
        }

        return clusterStates
    }

    /**
     * Extracts [OnOffCluster] attributes values
     *
     * @param clusterState cluster's state
     */
    private fun extractOnOffAttributeValue(clusterState: ClusterState): Boolean? {
        clusterState.attributeStates.forEach{(attributeId, attributeState) ->
            if(attributeId != ON_OFF_ATTRIBUTE_ID){
                return@forEach
            }
            return attributeState.value as Boolean
        }
        return null
    }

    /**
     * Extracts [ColorControlCluster] attributes values
     *
     * @param clusterState cluster's state
     */
    private fun extractColorControlAttributeValue(
        clusterState: ClusterState
    ): HSVColor ?{
        var hue = -1
        var saturation = -1
        clusterState.attributeStates.forEach{(attributeId, attributeState) ->
            when(attributeId){
                HUE_ATTRIBUTE_ID -> hue = attributeState.value as Int
                SATURATION_ATTRIBUTE_ID -> saturation = attributeState.value as Int
            }
            if(hue != -1 && saturation != -1){
                return@forEach
            }
        }

        return when(hue != -1 && saturation != -1){
            true -> HSVColor(
                AttributeUtils.mapValue(
                    hue,
                    AttributeUtils.MTR_MAX_HUE,
                    AttributeUtils.STD_MAX_HUE
                ),
                AttributeUtils.mapValue(
                    saturation,
                    AttributeUtils.MTR_MAX_SATURATION,
                    AttributeUtils.STD_MAX_SATURATION
                )
            )
            else -> null
        }
    }

    /**
     * Extracts [ColorControlCluster] attributes values
     *
     * @param clusterState cluster's state
     */
    private fun extractColorControlMiredsAttributeValue(
        clusterState: ClusterState
    ): Int ?{
        clusterState.attributeStates.forEach{(attributeId, attributeState) ->
            if(attributeId != MIREDS_ATTRIBUTE_ID) {
                return@forEach
            }

            return AttributeUtils.mapValue(
                attributeState.value as Int,
                AttributeUtils.MTR_MIN_COLOR_TEMPERATURE,
                AttributeUtils.MTR_MAX_COLOR_TEMPERATURE,
                AttributeUtils.STD_MIN_COLOR_TEMPERATURE,
                AttributeUtils.STD_MAX_COLOR_TEMPERATURE
            )
        }
        return null
    }

    /**
     * Extracts [FanControlCluster] fan mode attribute value
     *
     * @param clusterState cluster's state
     */
    private fun extractFanControlFanModeAttributeValue(
        clusterState: ClusterState
    ): FanMode ? {
        clusterState.attributeStates.forEach { (attributeId, attributeState) ->
            if(attributeId != FAN_MODE_ATTRIBUTE_ID) {
                return@forEach
            }
            return FanModeUtil.toEnum(attributeState.value as Int)
        }
        return null
    }

    /**
     * Extracts [FanControlCluster] percentage setting attribute value
     *
     * @param clusterState cluster's state
     */
    private fun extractFanControlPercentageSettingAttributeValue(
        clusterState: ClusterState
    ): Int ? {
        clusterState.attributeStates.forEach { (attributeId, attributeState) ->
            if(attributeId != PERCENTAGE_SETTING_ATTRIBUTE_ID) {
                return@forEach
            }
            return attributeState.value as Int
        }
        return null
    }

    /**
     * Extracts [LevelControlCluster] attributes values
     *
     * @param clusterState cluster's state
     */
    private fun extractLevelControlAttributeValue(clusterState: ClusterState): Int? {
        clusterState.attributeStates.forEach{(attributeId, attributeState) ->
            if(attributeId != CURRENT_LEVEL_ATTRIBUTE_ID){
                return@forEach
            }
            return AttributeUtils.mapValue(
                attributeState.value as Int,
                AttributeUtils.MTR_MAX_BRIGHTNESS,
                AttributeUtils.STD_MAX_BRIGHTNESS
            )
        }
        return null
    }
}
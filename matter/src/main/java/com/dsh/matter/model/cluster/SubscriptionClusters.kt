package com.dsh.matter.model.cluster

import chip.devicecontroller.ChipClusters

internal object SubscriptionClusters {
    /**
     * More values can be added to this map as new devices comes along
     */
    val map = setOf(
        ChipClusters.ColorControlCluster.CLUSTER_ID,
        ChipClusters.LevelControlCluster.CLUSTER_ID,
        ChipClusters.OnOffCluster.CLUSTER_ID,
        ChipClusters.FanControlCluster.CLUSTER_ID,
        ChipClusters.SwitchCluster.CLUSTER_ID,
    )
}
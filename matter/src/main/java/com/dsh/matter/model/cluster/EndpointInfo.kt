package com.dsh.matter.model.cluster

internal data class EndpointInfo(
    val endpoint: Int,
    val types: List<Long>,
    val serverClusters: List<Any> = arrayListOf(),
    val clientClusters: List<Any> = arrayListOf()
)

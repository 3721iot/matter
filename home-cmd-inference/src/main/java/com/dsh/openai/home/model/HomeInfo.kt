package com.dsh.openai.home.model

data class HomeInfo(
    /**
     * The home name
     */
    val name: String,

    /**
     * The home location
     */
    val location: HomeLocation,

    /**
     * The list of home names
     */
    val rooms: List<RoomInfo>
)
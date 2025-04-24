package com.dsh.tether.model

import com.dsh.matter.model.device.StateAttribute
import kotlin.collections.HashMap

data class TetherDevice(
    val id: Long,
    var name: String,
    val room: String,
    val home: String,
    val type: String,
    val states: HashMap<StateAttribute, Any>,
    val metadata: Any
)

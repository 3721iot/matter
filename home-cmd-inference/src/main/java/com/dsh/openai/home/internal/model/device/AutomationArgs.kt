package com.dsh.openai.home.internal.model.device

import com.dsh.openai.home.model.automation.Condition
import com.dsh.openai.home.model.automation.MatchType
import com.dsh.openai.home.model.automation.Task

data class AutomationArgs(
    /**
     * The name of the automation
     */
    val name: String,

    /**
     * The loops
     */
    val loops: String,

    /**
     * The match type
     */
    val matchType: MatchType,

    /**
     * The list of tasks
     */
    val tasks: MutableList<Task>,

    /**
     * The list of conditions
     */
    val conditions: MutableList<Condition>
)
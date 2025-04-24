package com.dsh.openai.home.internal.model.device

data class AutomationMgtArgs(
    /**
     * The management arguments
     */
    val intent: String,

    /**
     * The list of automation identifiers
     */
    val automationIds: List<String>
)

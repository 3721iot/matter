package com.dsh.openai.home.model.automation

enum class ManagementIntent(val intent: String) {
    /**
     * Delete
     */
    Delete("delete"),

    /**
     * Disable
     */
    Disable("disable"),

    /**
     * Enables  automation
     */
    Enable("enable"),

    /**
     * Trigger an automation
     */
    Trigger("trigger")
}
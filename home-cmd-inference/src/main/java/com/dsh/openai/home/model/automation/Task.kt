package com.dsh.openai.home.model.automation

data class Task(
    /**
     * The target identifier. This will mostly be the device identifier
     */
    val targetId: String,

    /**
     * The type of task to be carried out.
     */
    val controlIntent: String,

    /**
     * The task value
     */
    val value: String,
)

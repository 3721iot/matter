package com.dsh.openai.home

import androidx.lifecycle.LifecycleEventObserver

interface InferenceEngine : LifecycleEventObserver {

    /**
     * Start intent inference
     *
     * @param intent user's intent. Can be as simple as a single
     * device control to as complex as creating an automation
     */
    @Deprecated(
        message = "Use infer(intent: String, stream: Boolean)",
        replaceWith = ReplaceWith(
            "infer(intent: String, stream: Boolean)"
        ),
        level = DeprecationLevel.WARNING
    )
    suspend fun infer(intent: String)

    /**
     * Start intent inference
     *
     * @param intent user's intent. Can be as simple as a single
     * device control to as complex as creating an automation
     * @param stream whether to stream the responses or not.
     */
    suspend fun infer(intent: String, stream: Boolean)

    /**
     * Cancels intent inference.
     */
    suspend fun cancel()
}
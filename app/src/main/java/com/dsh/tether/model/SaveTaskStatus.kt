package com.dsh.tether.model

sealed class SaveTaskStatus {

    /**
     * The task hasn't been started
     */
    object NotStarted : SaveTaskStatus()

    /**
     * The task has been started but not yet completed
     */
    object  InProgress : SaveTaskStatus()

    /**
     * The task completed with an exception
     */
    class Failed(val msg: String, val ex: Exception) : SaveTaskStatus()

    /**
     * The task is completed successfully.
     */
    class Completed(val msg: String = "Done") : SaveTaskStatus()
}
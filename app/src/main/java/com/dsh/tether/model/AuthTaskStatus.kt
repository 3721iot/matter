package com.dsh.tether.model

open class AuthTaskStatus {
    /**
     * The task hasn't been started
     */
    object NotStarted : AuthTaskStatus()

    /**
     * The task has been started but not yet completed
     */
    object InProgress : AuthTaskStatus()

    /**
     * The task completed with an exception
     */
    class Failed(val msg: String, val ex: Exception? = null) : AuthTaskStatus()

    /**
     * The task is completed successfully.
     */
    class Completed(val msg: String = "Done") : AuthTaskStatus()
}
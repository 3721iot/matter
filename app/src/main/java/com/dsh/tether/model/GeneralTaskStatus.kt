package com.dsh.tether.model

open class GeneralTaskStatus {
    /**
     * The task hasn't been started
     */
    object NotStarted : GeneralTaskStatus()

    /**
     * The task has been started but not yet completed
     */
    object  InProgress : GeneralTaskStatus()

    /**
     * The task completed with an exception
     */
    class Failed(val msg: String, val ex: Exception? = null) : GeneralTaskStatus()

    /**
     * The task is completed successfully.
     */
    class Completed(val msg: String = "Done") : GeneralTaskStatus()
}
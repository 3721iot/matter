package com.dsh.tether.model

sealed class ValidationTaskStatus {

    /**
     * The task completed with an exception
     */
    class Failed(val msg: String, val ex: Exception?=null) : ValidationTaskStatus()

    /**
     * The task is completed successfully.
     */
    class Passed(val data: Any? = null) : ValidationTaskStatus()
}
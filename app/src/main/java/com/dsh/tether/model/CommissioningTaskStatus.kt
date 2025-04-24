package com.dsh.tether.model

import com.dsh.matter.model.CommissioningErrorCode

open class CommissioningTaskStatus {
    /**
     * The task hasn't been started
     */
    object NotStarted : CommissioningTaskStatus()

    /**
     * The task has been started but not yet completed
     */
    object  InProgress : CommissioningTaskStatus()

    /**
     * The task completed with an exception
     * @param error the error
     * @param msg the error message
     */
    class Failed(val error: CommissioningErrorCode, val msg: String) : CommissioningTaskStatus()

    /**
     * The task is in progress but needs user's intervention
     * @param error the error
     * @param msg the error message
     * @param data warning data
     */
    class Warning(val error: CommissioningErrorCode, val msg: String, val data: Any?) : CommissioningTaskStatus()

    /**
     * The task is completed successfully.
     *
     * @param msg a message to be displayed in the UI
     */
    class Completed(val msg: String? = "Done") : CommissioningTaskStatus()
}
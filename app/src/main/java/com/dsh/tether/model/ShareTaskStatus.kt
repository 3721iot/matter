package com.dsh.tether.model

import com.dsh.matter.model.CommissioningErrorCode

/**
 * An enumeration of statuses for an async [com.google.android.gms.tasks.Task]
 */
sealed class ShareTaskStatus {

    /**
     * The task hasn't been started
     */
    object NotStarted : ShareTaskStatus()

    /**
     * The task has been started but not yet completed
     */
    object  InProgress : ShareTaskStatus()

    /**
     * The task completed with an exception
     * @param error the error
     * @param msg the error message
     */
    class Failed(val error: CommissioningErrorCode, val msg: String) : ShareTaskStatus()

    /**
     * The task is in progress but needs user's intervention
     * @param error the error
     * @param msg the error message
     * @param data warning data
     */
    class Warning(val error: CommissioningErrorCode, val msg: String, val data: Any?) : ShareTaskStatus()

    /**
     * The task is completed successfully.
     *
     * @param msg a message to be displayed in the UI
     */
    class Completed(val msg: String? = "Done") : ShareTaskStatus()
}
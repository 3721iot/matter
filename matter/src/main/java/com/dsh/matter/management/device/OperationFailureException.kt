package com.dsh.matter.management.device

class OperationFailureException(
    message: String? = null, cause: Throwable? = null
) : Exception(message, cause)
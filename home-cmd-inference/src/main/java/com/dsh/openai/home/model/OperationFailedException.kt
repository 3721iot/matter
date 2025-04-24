package com.dsh.openai.home.model
class OperationFailedException(
    message: String? = null, cause: Throwable? = null
) : RuntimeException(message, cause)
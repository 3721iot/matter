package com.dsh.matter.model.scanner

@Deprecated(
    message = "This is never used",
    replaceWith = ReplaceWith("Use the respective exceptions"),
    level = DeprecationLevel.ERROR
)
enum class SetupPayloadError {
    /**
     * Unknown error
     */
    UNKNOWN_ERROR,

    /**
     * Invalid setup code
     */
    INVALID_SETUP_CODE,

    /**
     * Unrecognized QR code
     */
    UNRECOGNIZED_QR_CODE,

    /**
     * Invalid entry code
     */
    INVALID_ENTRY_CODE,
}
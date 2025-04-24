package com.dsh.matter.model

enum class CommissioningErrorCode {
    Unknown,
    DeviceNotFound,
    AttestationFailed,
    AddDeviceFailed,
    WindowOpenFailed,
    TypeIntrospectionFailed,
    ShareConfigFailed,
    PairingFailed,
    InvalidOnBoardingPayload,
    IncorrectState,
    InvalidDeviceIdentifier,
}
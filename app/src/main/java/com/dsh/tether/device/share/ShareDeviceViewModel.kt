package com.dsh.tether.device.share

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsh.matter.model.scanner.DeviceSharePayload
import com.dsh.matter.util.scanner.QrCodeGenerator
import com.dsh.matter.management.device.DeviceManager
import com.dsh.matter.model.CommissioningErrorCode
import com.dsh.tether.model.MtrDeviceMetadata
import com.dsh.tether.model.ShareTaskStatus
import com.dsh.tether.model.TetherDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShareDeviceViewModel @Inject constructor(
    private val deviceManager: DeviceManager
) : ViewModel() {

    /**
     * The QR code on-boarding payload live data
     */
    private val _shareDeviceQrCode = MutableLiveData<Bitmap?>()
    val shareDeviceQrCode: LiveData<Bitmap?>
        get() = _shareDeviceQrCode

    /**
     * The pairing code live data
     */
    private val _shareDevicePairingCode = MutableLiveData<String>()
    val shareDevicePairingCode: LiveData<String>
        get() = _shareDevicePairingCode

    /**
     * The device set up payload
     */
    private val _sharePayload = MutableLiveData<DeviceSharePayload>()
    val sharePayload : LiveData<DeviceSharePayload>
            get() = _sharePayload

    /**
     * The current status of a share device task
     */
    private val _shareDeviceStatus =  MutableLiveData<ShareTaskStatus>(ShareTaskStatus.NotStarted)
    val sharedDeviceStatus: LiveData<ShareTaskStatus>
        get() = _shareDeviceStatus

    /**
     * Share device
     *
     * @param thrDevice device
     */
    fun shareDevice(thrDevice: TetherDevice) {
        _shareDeviceStatus.postValue(ShareTaskStatus.InProgress)
        val metadata =  thrDevice.metadata as MtrDeviceMetadata
        viewModelScope.launch {
            try {
                val deviceSharePayload = deviceManager.shareDevice(
                    thrDevice.id,
                    metadata.discriminator,
                    SHARE_TIMEOUT_S
                )
                if(null == deviceSharePayload){
                    _shareDeviceStatus.postValue(
                        ShareTaskStatus.Failed(
                            CommissioningErrorCode.WindowOpenFailed,
                            "Failed to start device sharing"
                        )
                    )
                    return@launch
                }
                _sharePayload.postValue(deviceSharePayload!!)
            } catch (ex: Exception) {
                Timber.e("Failed to start device share. Cause: ${ex.localizedMessage}")
                val errMsg = "Failed to open the commission window"
                Timber.e(ex, errMsg)
                _shareDeviceStatus.postValue(
                    ShareTaskStatus.Failed(
                        error = CommissioningErrorCode.WindowOpenFailed, msg = errMsg
                    )
                )
                return@launch
            }
        }
    }

    /**
     * Generates Matter QR code from setup payload
     *
     * @param foregroundColor QR code's foreground color
     * @param backgroundColor QR code's background color
     */
    fun generateQRCodePayload(
        qrCodeContent: String,
        foregroundColor: Int,
        backgroundColor: Int
    ) {
        try {
            val bitmap = QrCodeGenerator.encodeQrCodeBitmap(
                    content = qrCodeContent,
                    width = QR_CODE_WIDTH,
                    height = QR_CODE_HEIGHT,
                    foregroundColor = foregroundColor,
                    backgroundColor = backgroundColor
                )
            _shareDeviceQrCode.postValue(bitmap)
        }catch (ex: Exception) {
            _shareDeviceQrCode.postValue(null)
        }
    }

    /**
     * Formats Matter pairing code
     *
     * @param manualEntryCode entry code
     */
    fun formatManualCodePayload(manualEntryCode: String) {
        val code =
            manualEntryCode.substring(0, 4) +
                    "–" + manualEntryCode.substring(4, 8) +
                    "–" + manualEntryCode.substring(8, 11)
        _shareDevicePairingCode.postValue(code)
    }

    companion object {

        // How long a commissioning window for Device Sharing should be open.
        private const val SHARE_TIMEOUT_S=180

        // QR Code width
        private const val QR_CODE_WIDTH = 400

        // QR Code height
        private const val QR_CODE_HEIGHT = 400
    }

}
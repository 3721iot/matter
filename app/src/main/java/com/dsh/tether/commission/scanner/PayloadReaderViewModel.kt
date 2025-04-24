package com.dsh.tether.commission.scanner

import androidx.camera.core.AspectRatio
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dsh.tether.model.GeneralTaskStatus
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class PayloadReaderViewModel @Inject constructor() : ViewModel() {

    /**
     * Setup payload live data
     */
    private val _setupPayload = MutableLiveData<String>()
    val setupPayload : LiveData<String>
        get() = _setupPayload

    /**
     * Scan QR code error
     */
    private val _scanQrCodeError = MutableLiveData<GeneralTaskStatus> ()
    val scanQrCodeError: LiveData<GeneralTaskStatus>
        get() = _scanQrCodeError

    private val barcodeScanner =
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().apply {
                setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            }.build())

    /**
     * Calculates the aspect ratio for the camera preview
     */
    fun getAspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    @ExperimentalGetImage
    fun processImageProxy(
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    _setupPayload.postValue(barcode.displayValue)
                }
            }
            .addOnFailureListener {
                Timber.e(it.message?: it.toString())
                _scanQrCodeError.postValue(GeneralTaskStatus.Failed("Failed to add lister", it))
            }.addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                imageProxy.close()
            }
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
package com.dsh.matter.util.scanner

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

object QrCodeGenerator {
    /**
     * Creates a QR code bitmap
     *
     * @param matrix the image matrix
     * @return the QR code bitmap
     */
    @JvmStatic
    private fun createQrCodeBitmap(
        matrix: BitMatrix,
        foregroundColor: Int,
        backgroundColor : Int
    ): Bitmap? {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) foregroundColor else backgroundColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Creates a QR code bitmap from a String
     *
     * @param content the QR code content
     * @param format the barcode format.
     * @param width the QR code width
     * @param height the QR code height
     * @return bitmap QR code
     */
    @Deprecated(
        message = "Use encodeQrCodeBitmap(content, width, height)",
        replaceWith = ReplaceWith("encodeQrCodeBitmap(content, width, height)"),
        level = DeprecationLevel.WARNING
    )
    @Throws(WriterException::class)
    @JvmStatic
    fun encodeQrCodeBitmap(
        content: String?,
        format: BarcodeFormat? = BarcodeFormat.QR_CODE,
        width: Int,
        height: Int,
        foregroundColor: Int,
        backgroundColor : Int
    ): Bitmap? {
        val bitmap = try {
            MultiFormatWriter().encode(content, format, width, height)
        } catch (e: WriterException) {
            throw e
        } catch (e: Exception) {
            throw WriterException(e)
        }
        return createQrCodeBitmap(bitmap, foregroundColor, backgroundColor)
    }

    /**
     * Creates a QR code bitmap from a String
     *
     * @param content the QR code content
     * @param width the QR code width
     * @param height the QR code height
     * @return bitmap QR code
     */
    @JvmStatic
    @Throws(WriterException::class)
    fun encodeQrCodeBitmap(
        content: String?,
        width: Int,
        height: Int,
        foregroundColor: Int,
        backgroundColor : Int
    ): Bitmap? {
        val bitmap = try {
            MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height)
        } catch (e: WriterException) {
            throw e
        } catch (e: Exception) {
            throw WriterException(e)
        }
        return createQrCodeBitmap(bitmap, foregroundColor, backgroundColor)
    }
}
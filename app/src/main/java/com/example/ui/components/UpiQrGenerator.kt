package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

object UpiQrGenerator {
    fun buildUpiPaymentUri(
        merchantUpiId: String,
        merchantName: String = "Royal Bullet 350",
        amount: Double,
        note: String = "Wallet Recharge"
    ): String {
        val cleanUpi = merchantUpiId.trim().ifBlank { "75449856@ybl" }
        val formattedAmount = String.format(Locale.US, "%.2f", amount.coerceAtLeast(1.0))
        return "upi://pay?pa=${Uri.encode(cleanUpi)}" +
            "&pn=${Uri.encode(merchantName)}" +
            "&am=$formattedAmount" +
            "&cu=INR" +
            "&tn=${Uri.encode(note)}"
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return runCatching {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) 0xFF0A3D91.toInt() else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }.getOrNull()
    }
}

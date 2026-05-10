package com.flowboard.presentation.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

fun imageUriToCompressedDataUrl(
    context: Context,
    uri: Uri,
    maxSide: Int = 256,
    quality: Int = 72
): String? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream with inJustDecodeBounds always returns null; only check if the stream opened.
        val streamOpened = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
            true
        } ?: false
        if (!streamOpened) return@runCatching null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return@runCatching null

        val resized = bitmap.scaleToMaxSide(maxSide)
        val bytes = ByteArrayOutputStream().use { output ->
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
        "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }.getOrNull()
}

private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
    var inSampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / inSampleSize >= maxSide && halfHeight / inSampleSize >= maxSide) {
        inSampleSize *= 2
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun Bitmap.scaleToMaxSide(maxSide: Int): Bitmap {
    val currentMaxSide = maxOf(width, height)
    if (currentMaxSide <= maxSide) return this
    val scale = maxSide.toFloat() / currentMaxSide.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

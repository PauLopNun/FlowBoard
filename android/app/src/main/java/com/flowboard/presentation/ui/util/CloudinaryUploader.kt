package com.flowboard.presentation.ui.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"

    // ── Fill these in with your Cloudinary credentials ──────────────────────
    const val CLOUD_NAME = "dwdzzucz6"
    const val UPLOAD_PRESET = "ml_default"
    // ────────────────────────────────────────────────────────────────────────

    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    val isConfigured: Boolean
        get() = CLOUD_NAME != "YOUR_CLOUD_NAME" && UPLOAD_PRESET != "YOUR_UPLOAD_PRESET"

    /**
     * Compress [uri] and upload to Cloudinary.
     * Returns the HTTPS URL of the uploaded image, or null on failure.
     */
    suspend fun uploadImage(context: Context, uri: Uri, maxSide: Int = 512, quality: Int = 82): String? =
        withContext(Dispatchers.IO) {
            val dataUrl = imageUriToCompressedDataUrl(context, uri, maxSide, quality)
            if (dataUrl == null) {
                Log.e(TAG, "Failed to compress image from URI $uri")
                return@withContext null
            }
            Log.d(TAG, "Uploading to Cloudinary (${dataUrl.length} chars base64)…")
            uploadDataUrl(dataUrl)
        }

    private fun uploadDataUrl(dataUrl: String): String? {
        val boundary = "FlowBoardBoundary${System.currentTimeMillis()}"
        return try {
            val conn = (URL(UPLOAD_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 30_000
                readTimeout = 30_000
            }

            val body = ByteArrayOutputStream()
            val w = body.bufferedWriter()
            w.write("--$boundary\r\n")
            w.write("Content-Disposition: form-data; name=\"file\"\r\n\r\n")
            w.write(dataUrl)
            w.write("\r\n--$boundary\r\n")
            w.write("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
            w.write(UPLOAD_PRESET)
            w.write("\r\n--$boundary--\r\n")
            w.flush()
            conn.outputStream.use { it.write(body.toByteArray()) }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText().also {
                    Log.e(TAG, "Cloudinary HTTP $responseCode: $it")
                }
                return null
            }

            Log.d(TAG, "Cloudinary response: ${responseText.take(200)}")
            val match = Regex(""""secure_url"\s*:\s*"([^"]+)"""").find(responseText)
            match?.groupValues?.get(1)?.replace("\\/", "/").also {
                if (it == null) Log.e(TAG, "secure_url not found in response")
                else Log.d(TAG, "Uploaded → $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary upload exception: ${e.message}", e)
            null
        }
    }
}

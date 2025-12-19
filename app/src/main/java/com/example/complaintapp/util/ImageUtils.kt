package com.example.complaintapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val MAX_IMAGE_DIMENSION = 800
    private const val TARGET_SIZE_KB = 100
    private const val JPEG_QUALITY = 70

    /**
     * Compress bitmap to target size (~100KB) for Firestore storage
     */
    suspend fun compressBitmap(bitmap: Bitmap, maxSizeKB: Int = TARGET_SIZE_KB): Bitmap = withContext(Dispatchers.IO) {
        var compressedBitmap = bitmap
        var quality = JPEG_QUALITY
        
        while (true) {
            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val sizeKB = outputStream.size() / 1024
            
            if (sizeKB <= maxSizeKB || quality <= 20) {
                Log.d(TAG, "Compressed image to ${sizeKB}KB with quality $quality")
                break
            }
            
            quality -= 10
            if (quality < 20) {
                // If still too large, scale down further
                val scaleFactor = 0.8f
                val newWidth = (compressedBitmap.width * scaleFactor).toInt()
                val newHeight = (compressedBitmap.height * scaleFactor).toInt()
                compressedBitmap = Bitmap.createScaledBitmap(compressedBitmap, newWidth, newHeight, true)
                quality = JPEG_QUALITY
            }
        }
        
        compressedBitmap
    }

    /**
     * Convert bitmap to Base64 string
     */
    suspend fun bitmapToBase64(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Convert Base64 string to bitmap
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64 to bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Load and scale bitmap from URI
     */
    suspend fun getScaledBitmap(uri: Uri, context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // First, decode with just bounds to get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Calculate sample size
                val scaleFactor = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                
                // Reset stream and decode with sample size
                inputStream.close()
                val newStream = context.contentResolver.openInputStream(uri)
                newStream?.use { newStream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = scaleFactor
                    }
                    BitmapFactory.decodeStream(newStream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: ${e.message}", e)
            null
        }
    }

    /**
     * Calculate sample size for downscaling
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}


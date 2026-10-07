package com.shatrughna.drivemate.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Robust photo loader for vehicle imagery:
 * - Downsamples large camera megapixels to display target to avoid OutOfMemoryError.
 * - Extracts EXIF orientation and normalizes rotation.
 * - Safe stream and file resolution for content:// and file:// schemes.
 */
object VehiclePhotoLoader {

    suspend fun loadOptimizedBitmap(
        context: Context,
        uriString: String?,
        maxDimension: Int = 1080
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) {
            return@withContext null
        }
        AppLogger.i(AppLogger.Tag.APP, "VehiclePhotoLoader: loading uriString=$uriString, maxDimension=$maxDimension")
        try {
            val bitmap = decodeSampledBitmap(context, uriString, maxDimension)
            if (bitmap == null) {
                AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: decodeSampledBitmap returned null for $uriString")
                return@withContext null
            }
            val rotatedBitmap = applyExifRotation(context, uriString, bitmap)
            AppLogger.i(AppLogger.Tag.APP, "VehiclePhotoLoader: successfully loaded ${rotatedBitmap.width}x${rotatedBitmap.height}")
            rotatedBitmap.asImageBitmap()
        } catch (e: Throwable) {
            AppLogger.e(AppLogger.Tag.APP, "VehiclePhotoLoader: Failed to load vehicle photo: ${e.message}", e)
            null
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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
        return inSampleSize.coerceAtLeast(1)
    }

    private fun openStream(context: Context, uriString: String): InputStream? {
        return try {
            if (uriString.startsWith("content://")) {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)
            } else {
                val file = if (uriString.startsWith("file://")) {
                    File(Uri.parse(uriString).path ?: "")
                } else {
                    File(uriString)
                }
                if (file.exists()) {
                    file.inputStream()
                } else {
                    val fallback = File(context.filesDir, file.name)
                    if (fallback.exists()) {
                        fallback.inputStream()
                    } else {
                        AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: file does not exist: ${file.absolutePath} or ${fallback.absolutePath}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: openStream failed for $uriString: ${e.message}")
            null
        }
    }

    private fun decodeSampledBitmap(
        context: Context,
        uriString: String,
        maxDimension: Int
    ): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val boundsStream = openStream(context, uriString) ?: run {
            AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: boundsStream is null for $uriString")
            return null
        }
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: invalid bounds ${boundsOptions.outWidth}x${boundsOptions.outHeight} for $uriString")
            return null
        }

        val sampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decodeStream = openStream(context, uriString) ?: run {
            AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: decodeStream is null for $uriString")
            return null
        }
        val decoded = decodeStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
        if (decoded == null) {
            AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: BitmapFactory.decodeStream returned null (sampleSize=$sampleSize)")
        }
        return decoded
    }

    private fun applyExifRotation(
        context: Context,
        uriString: String,
        bitmap: Bitmap
    ): Bitmap {
        val orientation = try {
            openStream(context, uriString)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: applyExifRotation exception: ${e.message}")
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (transformed != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            transformed
        } catch (e: Throwable) {
            AppLogger.w(AppLogger.Tag.APP, "VehiclePhotoLoader: createBitmap transformation failed: ${e.message}")
            bitmap
        }
    }
}

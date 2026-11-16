package ru.ecobirch.fa.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

data class LoadedLeafPhoto(
    val savedPath: String,
    val bitmap: Bitmap,
    val aspectRatio: Float,
    val displayName: String,
    val exifLatitude: Double?,
    val exifLongitude: Double?
)

/**
 * Stores compressed leaf photographs safely in app-private filesDir/leaf_photos/
 * instead of storing giant base64 strings inside preferences or SQLite rows.
 */
class PhotoStorageManager(private val context: Context) {

    private val photosDir: File
        get() = File(context.filesDir, "leaf_photos").apply { if (!exists()) mkdirs() }

    suspend fun importFromUri(uri: Uri, fallbackName: String = "leaf_${System.currentTimeMillis()}.jpg"): LoadedLeafPhoto? =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            var exifLat: Double? = null
            var exifLng: Double? = null
            var orientation = ExifInterface.ORIENTATION_NORMAL

            // 1. Read EXIF metadata before downscaling
            contentResolver.openInputStream(uri)?.use { exifStream ->
                runCatching {
                    val exif = ExifInterface(exifStream)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val latLong = exif.latLong
                    if (latLong != null && latLong.size == 2) {
                        exifLat = latLong[0]
                        exifLng = latLong[1]
                    }
                }
            }

            // 2. Decode bounds first to compute safe sampling
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return@withContext null

            val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, 1440, 1440)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val oriented = applyExifOrientation(rawBitmap, orientation)
            if (oriented !== rawBitmap) rawBitmap.recycle()

            val finalBitmap = scaleMaxSide(oriented, 1280)
            if (finalBitmap !== oriented) oriented.recycle()

            val fileName = "leaf_${UUID.randomUUID()}.jpg"
            val outFile = File(photosDir, fileName)
            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 86, fos)
            }

            val aspect = finalBitmap.width.toFloat() / finalBitmap.height.toFloat().coerceAtLeast(1f)
            LoadedLeafPhoto(
                savedPath = outFile.absolutePath,
                bitmap = finalBitmap,
                aspectRatio = aspect,
                displayName = fallbackName,
                exifLatitude = exifLat,
                exifLongitude = exifLng
            )
        }

    suspend fun loadBitmapFromPath(path: String, maxDim: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isBlank() || path == "template" || path == "stored_locally" || path == "stored_offline") {
            return@withContext null
        }
        val file = File(path)
        if (!file.exists()) return@withContext null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim, maxDim)
        }
        BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    suspend fun saveBase64DataUrl(dataUrl: String): String = withContext(Dispatchers.IO) {
        if (!dataUrl.startsWith("data:image")) return@withContext dataUrl
        val commaIdx = dataUrl.indexOf(',')
        if (commaIdx < 0) return@withContext ""
        val raw = dataUrl.substring(commaIdx + 1)
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        val outFile = File(photosDir, "leaf_imp_${UUID.randomUUID()}.jpg")
        outFile.writeBytes(bytes)
        outFile.absolutePath
    }

    fun encodeBitmapToJpegBase64(bitmap: Bitmap, maxDim: Int = 960, quality: Int = 82): String {
        val scaled = scaleMaxSide(bitmap, maxDim)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        if (scaled !== bitmap) scaled.recycle()
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    fun deletePhotoIfOwned(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.exists() && file.parentFile?.absolutePath == photosDir.absolutePath) {
            file.delete()
        }
    }

    fun createTempCaptureFile(): File {
        val cacheDir = File(context.cacheDir, "camera_captures").apply { if (!exists()) mkdirs() }
        return File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfH = height / 2
            val halfW = width / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun scaleMaxSide(bitmap: Bitmap, maxSide: Int): Bitmap {
        val maxCur = max(bitmap.width, bitmap.height)
        if (maxCur <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / maxCur.toFloat()
        val nw = max(1, (bitmap.width * ratio).roundToInt())
        val nh = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

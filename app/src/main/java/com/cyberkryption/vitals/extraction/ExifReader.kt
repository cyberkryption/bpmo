package com.cyberkryption.vitals.extraction

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.cyberkryption.vitals.core.ExifDates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

enum class ExifTag { DATE_TIME_ORIGINAL, DATE_TIME }

data class PhotoDate(val instant: Instant, val tag: ExifTag)

class ExifReader(private val context: Context) {
    /** DateTimeOriginal, then DateTime, each with its offset tag when present; else the device zone. */
    suspend fun read(uri: Uri): PhotoDate? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val zone = ZoneId.systemDefault()
                ExifDates.parse(
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                    exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
                    zone,
                )?.let { PhotoDate(it, ExifTag.DATE_TIME_ORIGINAL) }
                    ?: ExifDates.parse(
                        exif.getAttribute(ExifInterface.TAG_DATETIME),
                        exif.getAttribute(ExifInterface.TAG_OFFSET_TIME),
                        zone,
                    )?.let { PhotoDate(it, ExifTag.DATE_TIME) }
            }
        } catch (e: IOException) {
            null
        }
    }
}

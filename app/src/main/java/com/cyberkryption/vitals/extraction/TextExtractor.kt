package com.cyberkryption.vitals.extraction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.cyberkryption.vitals.core.Box
import com.cyberkryption.vitals.core.DisplayParser
import com.cyberkryption.vitals.core.GreyImage
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.SevenSegmentReader
import com.cyberkryption.vitals.core.TextBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** OCR words and the upright image size their boxes are measured against. */
data class OcrResult(val width: Int, val height: Int, val boxes: List<TextBox>)

/**
 * Reads a monitor photo: first as seven-segment digits, which most monitors show, then with ML Kit
 * Text Recognition v2 when that misses a value or is unsure of one, keeping whichever read more.
 */
class TextExtractor(private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** [type] is the monitor type when known; otherwise each reading is judged as the type it looks like. */
    suspend fun recognize(uri: Uri, type: ReadingType? = null): OcrResult? = withContext(Dispatchers.Default) {
        val bitmap = decodeUpright(uri, MAX_SIDE) ?: return@withContext null
        // Either reader failing leaves the other's result, rather than losing both.
        val segments = try {
            SevenSegmentReader.read(bitmap.toGreyImage())
        } catch (e: Exception) {
            Log.w(TAG, "Seven-segment reading failed", e)
            emptyList()
        }
        if (DisplayParser.readsClearly(segments, type)) return@withContext OcrResult(bitmap.width, bitmap.height, segments)
        val text = try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().toBoxes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Text recognition failed", e)
            emptyList()
        }
        OcrResult(bitmap.width, bitmap.height, DisplayParser.better(segments, text, type))
    }

    /** Decodes a downsampled bitmap, rotated per its EXIF orientation. */
    private fun decodeUpright(uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val TAG = "TextExtractor"
        const val MAX_SIDE = 2048
    }
}

/** Flattens ML Kit's blocks and lines into words with bounds and confidence. */
fun Text.toBoxes(): List<TextBox> = textBlocks.flatMap { it.lines }.flatMap { line ->
    line.elements.mapNotNull { element ->
        val r = element.boundingBox ?: return@mapNotNull null
        TextBox(
            text = element.text,
            bounds = Box(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat()),
            confidence = element.confidence,
        )
    }
}

fun Bitmap.toGreyImage(): GreyImage {
    val argb = IntArray(width * height)
    getPixels(argb, 0, width, 0, 0, width, height)
    return GreyImage.fromArgb(argb, width, height)
}

fun GreyImage.toBitmap(): Bitmap {
    val argb = IntArray(width * height) {
        val v = pixels[it].toInt() and 0xFF
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
}

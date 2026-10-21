package com.cyberkryption.vitals.extraction

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.cyberkryption.vitals.core.Box
import com.cyberkryption.vitals.core.DisplayParser
import com.cyberkryption.vitals.core.GreyImage
import com.cyberkryption.vitals.core.PreviewCrop
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.core.SevenSegmentReader
import com.cyberkryption.vitals.core.TextBox
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicReference

/** The guide frame, in the coordinates of the camera preview it's drawn over, and that preview's size. */
data class GuideFrame(val guide: Box, val previewWidth: Float, val previewHeight: Float)

/**
 * Reads the display inside the guide frame from each camera frame, on the analysis thread. A
 * frame's luminance plane is already greyscale, so it's cut to the guide, turned upright and read
 * as seven-segment digits, with ML Kit on the same cut-out when that misses a value or is unsure of one.
 */
class FrameReader(
    private val guide: AtomicReference<GuideFrame?>,
    private val type: () -> ReadingType,
    private val onRead: (List<TextBox>) -> Unit,
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var buffer = ByteArray(0)

    override fun analyze(image: ImageProxy) {
        onRead(image.use(::read))
    }

    fun close() = recognizer.close()

    private fun read(image: ImageProxy): List<TextBox> {
        val plane = image.planes[0]
        val data = plane.buffer.apply { rewind() }
        val size = data.remaining()
        if (buffer.size < size) buffer = ByteArray(size)
        data.get(buffer, 0, size)

        val rotation = image.imageInfo.rotationDegrees
        val sideways = rotation % 180 != 0
        val crop = guide.get()?.let {
            PreviewCrop.guideInFrame(
                it.guide, it.previewWidth, it.previewHeight,
                frameWidth = if (sideways) image.height else image.width,
                frameHeight = if (sideways) image.width else image.height,
            )
        }
        val grey = GreyImage.fromLuminance(buffer, image.width, image.height, plane.rowStride, plane.pixelStride, rotation, crop)

        val mode = type()
        val segments = SevenSegmentReader.read(grey)
        if (DisplayParser.readsClearly(segments, mode)) return segments
        val text = try {
            Tasks.await(recognizer.process(InputImage.fromBitmap(grey.toBitmap(), 0))).toBoxes()
        } catch (e: Exception) {
            emptyList()
        }
        return DisplayParser.better(segments, text, mode)
    }
}

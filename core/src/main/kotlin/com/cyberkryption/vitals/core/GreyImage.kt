package com.cyberkryption.vitals.core

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** An 8-bit greyscale image, row by row, as the display reader works on it. */
class GreyImage(val width: Int, val height: Int, val pixels: ByteArray) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height) { "Bad size ${width}x$height" }
    }

    operator fun get(x: Int, y: Int): Int = pixels[y * width + x].toInt() and 0xFF

    /** Shrinks to [newWidth]×[newHeight] by averaging each source area, so thin segments aren't lost. */
    fun scaled(newWidth: Int, newHeight: Int): GreyImage {
        if (newWidth == width && newHeight == height) return this
        val sum = integral()
        val out = ByteArray(newWidth * newHeight)
        for (y in 0 until newHeight) {
            val y0 = y * height / newHeight
            val y1 = max(y0 + 1, (y + 1) * height / newHeight)
            for (x in 0 until newWidth) {
                val x0 = x * width / newWidth
                val x1 = max(x0 + 1, (x + 1) * width / newWidth)
                out[y * newWidth + x] = (areaSum(sum, x0, y0, x1, y1) / ((x1 - x0) * (y1 - y0))).toByte()
            }
        }
        return GreyImage(newWidth, newHeight, out)
    }

    /** Shrinks so neither side exceeds [maxSide], keeping the aspect ratio. */
    fun fitWithin(maxSide: Int): GreyImage {
        val scale = maxSide.toFloat() / max(width, height)
        if (scale >= 1f) return this
        return scaled(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
    }

    /** Light for dark: a display lit the other way reads like a normal one. */
    fun inverted(): GreyImage = GreyImage(width, height, ByteArray(pixels.size) { (pixels[it].toInt() xor 0xFF).toByte() })

    /** Bilinear sample at a fractional pixel centre, clamped to the edges. */
    internal fun sample(x: Float, y: Float): Float {
        val fx = (x - 0.5f).coerceIn(0f, width - 1f)
        val fy = (y - 0.5f).coerceIn(0f, height - 1f)
        val x0 = floor(fx).toInt()
        val y0 = floor(fy).toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        val tx = fx - x0
        val ty = fy - y0
        val top = this[x0, y0] + (this[x1, y0] - this[x0, y0]) * tx
        val bottom = this[x0, y1] + (this[x1, y1] - this[x0, y1]) * tx
        return top + (bottom - top) * ty
    }

    /** Summed-area table with a zero first row and column: (width + 1) × (height + 1). */
    private fun integral(): IntArray {
        val w = width + 1
        val sum = IntArray(w * (height + 1))
        for (y in 0 until height) {
            var row = 0
            for (x in 0 until width) {
                row += this[x, y]
                sum[(y + 1) * w + x + 1] = sum[y * w + x + 1] + row
            }
        }
        return sum
    }

    private fun areaSum(sum: IntArray, x0: Int, y0: Int, x1: Int, y1: Int): Int {
        val w = width + 1
        return sum[y1 * w + x1] - sum[y0 * w + x1] - sum[y1 * w + x0] + sum[y0 * w + x0]
    }

    companion object {
        /** From packed ARGB pixels (an Android bitmap or a JVM image), weighting green as the eye does. */
        fun fromArgb(argb: IntArray, width: Int, height: Int): GreyImage {
            val out = ByteArray(width * height)
            for (i in out.indices) {
                val c = argb[i]
                out[i] = ((((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8).toByte()
            }
            return GreyImage(width, height, out)
        }

        /**
         * From a camera frame's luminance (Y) plane, turned upright by [rotationDegrees] (clockwise, as
         * CameraX reports it) and cut to [crop], given in upright coordinates. No crop keeps the whole frame.
         */
        fun fromLuminance(
            plane: ByteArray,
            width: Int,
            height: Int,
            rowStride: Int,
            pixelStride: Int,
            rotationDegrees: Int,
            crop: Box? = null,
        ): GreyImage {
            val rotation = ((rotationDegrees % 360) + 360) % 360
            require(rotation % 90 == 0) { "Rotation $rotationDegrees isn't a quarter turn" }
            val uprightWidth = if (rotation % 180 == 0) width else height
            val uprightHeight = if (rotation % 180 == 0) height else width
            val left = (crop?.left?.roundToInt() ?: 0).coerceIn(0, uprightWidth - 1)
            val top = (crop?.top?.roundToInt() ?: 0).coerceIn(0, uprightHeight - 1)
            val right = (crop?.right?.roundToInt() ?: uprightWidth).coerceIn(left + 1, uprightWidth)
            val bottom = (crop?.bottom?.roundToInt() ?: uprightHeight).coerceIn(top + 1, uprightHeight)
            val outWidth = right - left
            val out = ByteArray(outWidth * (bottom - top))
            for (uy in top until bottom) {
                for (ux in left until right) {
                    val index = when (rotation) {
                        0 -> uy * rowStride + ux * pixelStride
                        90 -> (height - 1 - ux) * rowStride + uy * pixelStride
                        180 -> (height - 1 - uy) * rowStride + (width - 1 - ux) * pixelStride
                        else -> ux * rowStride + (width - 1 - uy) * pixelStride
                    }
                    out[(uy - top) * outWidth + ux - left] = plane[index]
                }
            }
            return GreyImage(outWidth, bottom - top, out)
        }
    }
}

/** Where a guide frame drawn over the camera preview falls in the camera frame itself. */
object PreviewCrop {
    /**
     * Maps [guide], in the coordinates of a preview of [viewWidth]×[viewHeight] that fills itself with
     * the upright frame of [frameWidth]×[frameHeight] and crops the overflow evenly (PreviewView's
     * FILL_CENTER), to frame coordinates. It's grown by [margin] of its size on each side, so a display
     * just over the edge of the guide still counts, and kept inside the frame.
     */
    fun guideInFrame(
        guide: Box,
        viewWidth: Float,
        viewHeight: Float,
        frameWidth: Int,
        frameHeight: Int,
        margin: Float = 0.1f,
    ): Box {
        val scale = max(viewWidth / frameWidth, viewHeight / frameHeight)
        val offsetX = (viewWidth - frameWidth * scale) / 2
        val offsetY = (viewHeight - frameHeight * scale) / 2
        val growX = guide.width * margin
        val growY = guide.height * margin
        return Box(
            left = ((guide.left - growX - offsetX) / scale).coerceIn(0f, frameWidth.toFloat()),
            top = ((guide.top - growY - offsetY) / scale).coerceIn(0f, frameHeight.toFloat()),
            right = ((guide.right + growX - offsetX) / scale).coerceIn(0f, frameWidth.toFloat()),
            bottom = ((guide.bottom + growY - offsetY) / scale).coerceIn(0f, frameHeight.toFloat()),
        )
    }
}

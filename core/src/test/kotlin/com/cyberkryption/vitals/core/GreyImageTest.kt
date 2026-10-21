package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GreyImageTest {
    /** A 3 × 2 frame stored with one byte of row padding, reading 1 2 3 / 4 5 6. */
    private val plane = byteArrayOf(1, 2, 3, 0, 4, 5, 6, 0)

    private fun GreyImage.rows() = (0 until height).map { y -> (0 until width).map { x -> this[x, y] } }

    @Test
    fun turnsCameraFramesUpright() {
        fun upright(rotation: Int) = GreyImage.fromLuminance(plane, 3, 2, rowStride = 4, pixelStride = 1, rotationDegrees = rotation).rows()
        assertEquals(listOf(listOf(1, 2, 3), listOf(4, 5, 6)), upright(0))
        assertEquals(listOf(listOf(4, 1), listOf(5, 2), listOf(6, 3)), upright(90))
        assertEquals(listOf(listOf(6, 5, 4), listOf(3, 2, 1)), upright(180))
        assertEquals(listOf(listOf(3, 6), listOf(2, 5), listOf(1, 4)), upright(270))
    }

    @Test
    fun cropsInUprightCoordinates() {
        val crop = GreyImage.fromLuminance(plane, 3, 2, 4, 1, rotationDegrees = 90, crop = Box(1f, 1f, 2f, 3f))
        assertEquals(listOf(listOf(2), listOf(3)), crop.rows())
    }

    @Test
    fun followsPixelStride() {
        val interleaved = byteArrayOf(7, 0, 8, 0, 9, 0, 10, 0)
        val image = GreyImage.fromLuminance(interleaved, 2, 2, rowStride = 4, pixelStride = 2, rotationDegrees = 0)
        assertEquals(listOf(listOf(7, 8), listOf(9, 10)), image.rows())
    }

    @Test
    fun weightsColoursByBrightness() {
        val image = GreyImage.fromArgb(intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt()), 2, 2)
        assertEquals(listOf(listOf(255, 0), listOf(149, 28)), image.rows())
    }

    @Test
    fun shrinksByAveraging() {
        val image = GreyImage(4, 2, byteArrayOf(0, 100, 50, 50, 100, 200.toByte(), 50, 50))
        assertEquals(listOf(listOf(100, 50)), image.scaled(2, 1).rows())
        assertEquals(2, image.fitWithin(2).width)
        assertTrue(image.fitWithin(10) === image)
    }

    @Test
    fun guideFrameMapsToTheCameraFrame() {
        // A 1080 × 2400 preview filled by a 960 × 1280 frame: scaled by 1.875, 360 px cropped each side.
        fun near(box: Box, left: Float, top: Float, right: Float, bottom: Float) = assertTrue(
            listOf(box.left - left, box.top - top, box.right - right, box.bottom - bottom).all { kotlin.math.abs(it) < 0.01f },
            "$box",
        )
        near(PreviewCrop.guideInFrame(Box(140f, 600f, 940f, 1666f), 1080f, 2400f, 960, 1280, margin = 0f), 266.667f, 320f, 693.333f, 888.533f)
        // The whole preview shows frame x 192–768; a 10% margin widens that, and the frame clips it.
        near(PreviewCrop.guideInFrame(Box(0f, 0f, 1080f, 2400f), 1080f, 2400f, 960, 1280), 134.4f, 0f, 825.6f, 1280f)
    }

    @Test
    fun quadMapsTheUnitSquareToItsCorners() {
        val quad = Quad(Point(10f, 20f), Point(110f, 30f), Point(120f, 220f), Point(0f, 200f))
        fun near(p: Point, x: Float, y: Float) = assertTrue(kotlin.math.abs(p.x - x) < 0.01f && kotlin.math.abs(p.y - y) < 0.01f, "$p")
        near(quad.map(0f, 0f), 10f, 20f)
        near(quad.map(1f, 0f), 110f, 30f)
        near(quad.map(1f, 1f), 120f, 220f)
        near(quad.map(0f, 1f), 0f, 200f)
        near(Quad.of(Box(0f, 0f, 100f, 50f)).map(0.5f, 0.5f), 50f, 25f)
    }
}

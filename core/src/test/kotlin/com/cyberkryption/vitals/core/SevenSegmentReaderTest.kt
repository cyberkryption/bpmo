package com.cyberkryption.vitals.core

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Polygon
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SevenSegmentReaderTest {
    private fun bp(image: BufferedImage): List<Int?> {
        val d = DisplayParser.parse(SevenSegmentReader.read(image.toGrey()), ReadingType.BP).detections
        return listOf(d[Field.SYSTOLIC]?.value, d[Field.DIASTOLIC]?.value, d[Field.PULSE]?.value)
    }

    @Test
    fun readsEveryDigit() {
        for ((sys, dia, pulse) in READINGS) {
            assertEquals(listOf(sys, dia, pulse), bp(monitor(sys, dia, pulse)), "$sys/$dia $pulse")
        }
    }

    @Test
    fun clearDigitsAreConfident() {
        val boxes = SevenSegmentReader.read(monitor(154, 100, 76).toGrey())
        val d = DisplayParser.parse(boxes, ReadingType.BP).detections
        assertTrue(d.values.none { it.isLowConfidence }, d.toString())
    }

    @Test
    fun ignoresTheLevelBarClockAndIcons() {
        val texts = SevenSegmentReader.read(monitor(154, 100, 76).toGrey()).map { it.text }
        assertTrue("154" in texts && "100" in texts && "76" in texts, texts.toString())
        assertFalse(texts.any { it.length > 3 }, texts.toString())
    }

    @Test
    fun boundsAreInTheOriginalImage() {
        val image = monitor(154, 100, 76)
        val sys = SevenSegmentReader.read(image.toGrey()).first { it.text == "154" }.bounds
        // The systolic digits' cells run from x 431 to 850, y 520 to 750.
        assertTrue(sys.left in 300f..520f && sys.right in 820f..900f, sys.toString())
        assertTrue(sys.top in 490f..560f && sys.bottom in 720f..790f, sys.toString())
    }

    @Test
    fun copesWithHowPhotosComeOut() {
        val base = monitor(128, 83, 69)
        val variants = mapOf(
            "tilted 7° left" to rotate(base, -7.0),
            "tilted 7° right" to rotate(base, 7.0),
            "skewed" to shear(base, 0.12),
            "far away" to farAway(base),
            "small (300 x 400)" to scale(base, 300, 400),
            "dim" to adjust(base) { it * 0.4f },
            "low contrast" to adjust(base) { 128 + (it - 128) * 0.45f },
            "blurred" to blur(blur(base)),
            "heavy JPEG" to jpeg(base, 0.3f),
            "glare" to glare(base),
        )
        val failures = variants.mapNotNull { (name, image) ->
            bp(image).takeIf { it != listOf(128, 83, 69) }?.let { "$name read $it" }
        }
        assertTrue(failures.isEmpty(), failures.joinToString())
    }

    /** Layouts and lighting seen in real photos of a monitor that once defeated the reader. */
    @Test
    fun copesWithRealWorldLayouts() {
        val cases = mapOf(
            // The level bar beside DIA starts in the gap between the SYS and DIA rows.
            "level bar between rows" to (monitor(148, 99, 78, barTop = 730) to listOf(148, 99, 78)),
            // Lit from above, the foot of the display is dimmer than the wall behind (greys as measured).
            "display dim at the foot" to (monitor(128, 82, 107, lcdBottom = 80, wall = 235, bezel = 10) to listOf(128, 82, 107)),
            "reflection joining display and wall" to (monitor(162, 103, 79, reflection = true) to listOf(162, 103, 79)),
            "all of these, tilted" to (rotate(monitor(144, 99, 84, barTop = 730, lcdBottom = 80, wall = 230, bezel = 10, reflection = true), 6.0) to listOf(144, 99, 84)),
        )
        val failures = cases.mapNotNull { (name, case) ->
            val (image, expected) = case
            bp(image).takeIf { it != expected }?.let { "$name read $it" }
        }
        assertTrue(failures.isEmpty(), failures.joinToString())
    }

    @Test
    fun copesWithAShadowAcrossTheDisplay() {
        // In a dim room, the edge of the phone's shadow falls across the top of the systolic digits.
        for (edge in listOf(515, 535, 560, 580)) {
            assertEquals(listOf(158, 105, 78), bp(monitor(158, 105, 78, wall = 90, shadowFrom = edge)), "shadow from $edge")
        }
    }

    @Test
    fun readsAnOximetersLitScreen() {
        val failures = OXIMETER_READINGS.flatMap { (spo2, pulse) ->
            TYPEFACES.mapNotNull { face ->
                val d = DisplayParser.parse(SevenSegmentReader.read(oximeter(spo2, pulse, face).toGrey()), ReadingType.OXIMETER).detections
                val read = listOf(d[Field.SPO2]?.value, d[Field.PULSE]?.value)
                "$face $spo2/$pulse read $read".takeIf { read != listOf(spo2, pulse) }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString())
    }

    @Test
    fun aLitScreenFarAwayOrTiltedIsRead() {
        val base = oximeter(96, 74, "SansSerif")
        for ((name, image) in mapOf("far away" to farAway(base), "tilted" to rotate(base, 5.0), "blurred" to blur(base))) {
            val d = DisplayParser.parse(SevenSegmentReader.read(image.toGrey()), ReadingType.OXIMETER).detections
            assertEquals(listOf(96, 74), listOf(d[Field.SPO2]?.value, d[Field.PULSE]?.value), name)
        }
    }

    @Test
    fun aLitScreensLabelsAndSmallPrintAreNotValues() {
        // Labels like SpO2 and PRbpm, and the perfusion index 4.7, sit beside the values.
        val texts = SevenSegmentReader.read(oximeter(97, 72, "Serif").toGrey()).map { it.text }
        assertEquals(listOf("97", "72"), texts)
    }

    @Test
    fun aCloseUpWithoutBezelIsReadWhole() {
        val base = monitor(136, 92, 88)
        // Just the inside of the display, as a tight crop of the camera frame gives.
        assertEquals(listOf(136, 92, 88), bp(base.getSubimage(320, 440, 560, 750)))
    }

    @Test
    fun blankImageReadsNothing() {
        val blank = BufferedImage(400, 600, BufferedImage.TYPE_INT_RGB)
        assertTrue(SevenSegmentReader.read(blank.toGrey()).isEmpty())
    }

    /**
     * Photos named like `bp-154-100-76.jpg` or `ox-97-68.jpg` (upright, any suffix after the values)
     * in the test resources' `displays` folder, or in a folder given with
     * `./gradlew :core:test -PdisplayPhotos=/path`, must read as their names say.
     */
    @Test
    fun readsRealPhotos() {
        val folders = listOfNotNull(
            javaClass.getResource("/displays")?.toURI()?.let(::File),
            System.getProperty("displayPhotos")?.let(::File),
        )
        val named = Regex("(bp-\\d+-\\d+-\\d+|ox-\\d+-\\d+)(-.*)?")
        val photos = folders.flatMap { it.listFiles()?.toList().orEmpty() }
            .filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png") && named.matches(it.nameWithoutExtension) }
        val failures = photos.mapNotNull { photo ->
            val parts = photo.nameWithoutExtension.split('-')
            val type = if (parts[0] == "ox") ReadingType.OXIMETER else ReadingType.BP
            val expected = parts.drop(1).take(type.fields.size).map { it.toInt() }
            val d = DisplayParser.parse(SevenSegmentReader.read(ImageIO.read(photo).toGrey()), type).detections
            val read = type.fields.map { d[it]?.value }
            "${photo.name} read $read".takeIf { read != expected }
        }
        println("Read ${photos.size - failures.size} of ${photos.size} display photos")
        assertTrue(failures.isEmpty(), failures.joinToString())
    }

    private companion object {


        val READINGS = listOf(
            Triple(154, 100, 76), Triple(128, 83, 69), Triple(97, 58, 112),
            Triple(203, 119, 45), Triple(111, 71, 60), Triple(136, 92, 88),
        )

        /** SpO₂ and pulse readings using every digit. */
        val OXIMETER_READINGS = listOf(97 to 72, 88 to 105, 94 to 63, 91 to 58, 86 to 120)

        /** Java's stand-ins for the typefaces oximeters use: serif, sans and monospaced. */
        val TYPEFACES = listOf("Serif", "SansSerif", "Monospaced")

        fun BufferedImage.toGrey(): GreyImage {
            val argb = getRGB(0, 0, width, height, null, 0, width)
            return GreyImage.fromArgb(argb, width, height)
        }

        /** Lit segments a–g for each digit. */
        val SEGMENTS = listOf("abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg")

        /**
         * A stand-in BP monitor photo: a wall, a dark bezel, an LCD lit unevenly from the top, the
         * clock, SYS and DIA right-aligned in large digits, a level bar of three near-square blocks
         * starting at [barTop], a heart icon and the pulse in smaller digits. [lcdBottom] is the LCD's grey at
         * its foot, [wall] the wall's at the top and [bezel] the bezel's, and [reflection] adds a
         * light's reflection across the bezel joining the LCD to the wall. [shadowFrom] darkens
         * everything below that row, as a shadow with a sharp edge does.
         */
        fun monitor(
            sys: Int,
            dia: Int,
            pulse: Int,
            barTop: Int = 530,
            lcdBottom: Int = 118,
            wall: Int = 200,
            bezel: Int = 28,
            reflection: Boolean = false,
            shadowFrom: Int? = null,
        ): BufferedImage {
            val image = BufferedImage(1200, 1600, BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.paint = GradientPaint(0f, 0f, Color(wall, wall - 10, wall - 5), 0f, 1600f, Color(70, 65, 70))
            g.fillRect(0, 0, 1200, 1600)
            g.color = Color(bezel, bezel, bezel + 2)
            g.fillRoundRect(150, 250, 900, 1250, 120, 120)
            g.paint = GradientPaint(0f, 420f, Color(212, 214, 208), 0f, 1210f, Color(lcdBottom, lcdBottom + 2, lcdBottom - 2))
            g.fillRect(300, 420, 600, 790)
            if (reflection) {
                // A streak of light across the bezel's corner, from the wall to the LCD.
                g.color = Color(240, 240, 238)
                g.stroke = java.awt.BasicStroke(44f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
                g.drawLine(120, 300, 320, 450)
                g.stroke = java.awt.BasicStroke(1f)
            }
            g.color = Color(52, 56, 60)
            number(g, 12, right = 520, top = 440, height = 40)
            g.fillRect(535, 450, 6, 6)
            g.fillRect(535, 468, 6, 6)
            number(g, 34, right = 620, top = 440, height = 40)
            number(g, sys, right = 850, top = 520, height = 230)
            number(g, dia, right = 850, top = 790, height = 230)
            number(g, pulse, right = 850, top = 1050, height = 110)
            for (i in 0 until 3) g.fillRect(860, barTop + i * 60, 30, 42)
            g.fillOval(560, 1080, 50, 45)
            g.fillRect(330, 1140, 40, 18)
            if (shadowFrom != null) {
                g.color = Color(0, 0, 0, 140)
                g.fillRect(0, shadowFrom, 1200, 1600 - shadowFrom)
            }
            g.dispose()
            return image
        }

        /**
         * A stand-in fingertip oximeter photo: a white body on a desk, a black face, and a screen
         * that makes its own light, framed by a thin outline: labels, SpO₂ and pulse in a bold
         * [typeface], the perfusion index in small print between them, a pulse wave and a heart.
         */
        fun oximeter(spo2: Int, pulse: Int, typeface: String): BufferedImage {
            val image = BufferedImage(1400, 1000, BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            fun font(size: Float) = java.awt.Font(typeface, java.awt.Font.BOLD, 10).deriveFont(size)
            // Laid out left to right from the typeface's own widths: SpO₂, the perfusion index, the pulse.
            val left = 330
            val spo2Width = g.getFontMetrics(font(170f)).stringWidth(spo2.toString())
            val indexLeft = left + spo2Width + 20
            val pulseLeft = indexLeft + g.getFontMetrics(font(28f)).stringWidth("PI%") + 30
            val right = pulseLeft + g.getFontMetrics(font(125f)).stringWidth(pulse.toString()) + 30
            g.paint = GradientPaint(0f, 0f, Color(90, 80, 100), 0f, 1000f, Color(40, 36, 44))
            g.fillRect(0, 0, 1400, 1000)
            g.color = Color(226, 226, 222)
            g.fillRoundRect(100, 220, right + 70, 560, 260, 260)
            g.color = Color(16, 16, 18)
            g.fillRoundRect(240, 280, right - 110, 440, 200, 200)
            g.color = Color(110, 110, 112)
            g.stroke = java.awt.BasicStroke(5f)
            g.drawRoundRect(left - 25, 330, right - left + 25, 340, 30, 30)
            g.drawOval(right + 40, 440, 60, 110)
            val yellow = Color(232, 220, 30)
            val cyan = Color(40, 200, 250)
            fun text(value: String, color: Color, size: Float, x: Int, y: Int) {
                g.color = color
                g.font = font(size)
                g.drawString(value, x, y)
            }
            text("%SpO", yellow, 36f, left, 390)
            text("2", yellow, 22f, left + g.getFontMetrics(font(36f)).stringWidth("%SpO") + 2, 396)
            text("PRbpm", yellow, 36f, pulseLeft, 390)
            text(spo2.toString(), cyan, 170f, left, 570)
            text("PI%", cyan, 28f, indexLeft, 480)
            text("4.7", cyan, 28f, indexLeft, 525)
            text(pulse.toString(), cyan, 125f, pulseLeft, 560)
            g.color = yellow
            g.stroke = java.awt.BasicStroke(8f)
            val wave = Polygon()
            for (x in left..right - 90 step 10) wave.addPoint(x, 635 - (25 * Math.abs(Math.sin(x / 40.0))).toInt())
            g.drawPolyline(wave.xpoints, wave.ypoints, wave.npoints)
            g.color = Color(230, 50, 40)
            g.fillOval(right - 70, 605, 26, 26)
            g.fillOval(right - 48, 605, 26, 26)
            g.dispose()
            return image
        }

        fun number(g: java.awt.Graphics2D, value: Int, right: Int, top: Int, height: Int) {
            val width = height * 0.52f
            val pitch = height * 0.65f
            for ((i, c) in value.toString().reversed().withIndex()) {
                digit(g, SEGMENTS[c - '0'], right - width - i * pitch, top.toFloat(), width, height.toFloat())
            }
        }

        fun digit(g: java.awt.Graphics2D, lit: String, x: Float, y: Float, w: Float, h: Float) {
            val t = h * 0.105f
            val gap = t / 2 + h * 0.012f
            fun bar(x1: Float, y1: Float, x2: Float, y2: Float) {
                val horizontal = y1 == y2
                val half = t / 2
                val p = Polygon()
                if (horizontal) {
                    listOf(x1 to y1, x1 + half to y1 - half, x2 - half to y1 - half, x2 to y1, x2 - half to y1 + half, x1 + half to y1 + half)
                } else {
                    listOf(x1 to y1, x1 + half to y1 + half, x1 + half to y2 - half, x1 to y2, x1 - half to y2 - half, x1 - half to y1 + half)
                }.forEach { (px, py) -> p.addPoint(px.toInt(), py.toInt()) }
                g.fillPolygon(p)
            }
            val left = x + t / 2
            val rightX = x + w - t / 2
            if ('a' in lit) bar(x + gap, y + t / 2, x + w - gap, y + t / 2)
            if ('b' in lit) bar(rightX, y + gap, rightX, y + h / 2 - gap / 2)
            if ('c' in lit) bar(rightX, y + h / 2 + gap / 2, rightX, y + h - gap)
            if ('d' in lit) bar(x + gap, y + h - t / 2, x + w - gap, y + h - t / 2)
            if ('e' in lit) bar(left, y + h / 2 + gap / 2, left, y + h - gap)
            if ('f' in lit) bar(left, y + gap, left, y + h / 2 - gap / 2)
            if ('g' in lit) bar(x + gap, y + h / 2, x + w - gap, y + h / 2)
        }

        fun transform(source: BufferedImage, transform: AffineTransform): BufferedImage {
            val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
            val g = out.createGraphics()
            g.color = Color(40, 38, 40)
            g.fillRect(0, 0, out.width, out.height)
            g.drawImage(source, AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR), 0, 0)
            g.dispose()
            return out
        }

        fun rotate(source: BufferedImage, degrees: Double) =
            transform(source, AffineTransform.getRotateInstance(Math.toRadians(degrees), source.width / 2.0, source.height / 2.0))

        fun shear(source: BufferedImage, amount: Double) =
            transform(source, AffineTransform().apply { translate(-amount * source.height / 2, 0.0); shear(amount, 0.0) })

        fun farAway(source: BufferedImage) =
            transform(source, AffineTransform().apply { translate(source.width / 3.0, source.height / 3.0); scale(1 / 3.0, 1 / 3.0) })

        fun scale(source: BufferedImage, width: Int, height: Int): BufferedImage {
            val out = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val g = out.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(source, 0, 0, width, height, null)
            g.dispose()
            return out
        }

        fun adjust(source: BufferedImage, level: (Float) -> Float): BufferedImage {
            val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    val c = source.getRGB(x, y)
                    fun ch(shift: Int) = level(((c shr shift) and 0xFF).toFloat()).toInt().coerceIn(0, 255) shl shift
                    out.setRGB(x, y, ch(16) or ch(8) or ch(0))
                }
            }
            return out
        }

        fun blur(source: BufferedImage): BufferedImage {
            val kernel = Kernel(5, 5, FloatArray(25) { 1 / 25f })
            return ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null).filter(source, null)
        }

        fun jpeg(source: BufferedImage, quality: Float): BufferedImage {
            val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
            val bytes = ByteArrayOutputStream()
            ImageIO.createImageOutputStream(bytes).use { out ->
                writer.output = out
                val param = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = quality
                }
                writer.write(null, IIOImage(source, null, null), param)
            }
            writer.dispose()
            return ImageIO.read(ByteArrayInputStream(bytes.toByteArray()))
        }

        /** A bright reflection on the display, clear of the digits, like a ceiling light. */
        fun glare(source: BufferedImage): BufferedImage {
            val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
            val g = out.createGraphics()
            g.drawImage(source, 0, 0, null)
            g.composite = AlphaComposite.SrcOver
            g.paint = RadialGradientPaint(420f, 1060f, 100f, floatArrayOf(0f, 1f), arrayOf(Color(255, 255, 255, 230), Color(255, 255, 255, 0)))
            g.fillOval(320, 960, 200, 200)
            g.dispose()
            return out
        }
    }
}

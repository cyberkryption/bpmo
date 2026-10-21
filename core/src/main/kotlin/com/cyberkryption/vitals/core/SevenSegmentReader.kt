package com.cyberkryption.vitals.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Reads the seven-segment digits on most BP monitors without a text-recognition model, which
 * struggles with digits made of separate bars.
 *
 * It finds the display (the biggest bright, solid patch), straightens and enlarges it, evens out
 * the lighting, separates the dark segments from the background, splits rows and digits, and
 * reads each digit from which of its seven segments are dark. Each number of two or more digits
 * comes back with its bounds in the input image, for [DisplayParser] to assign to fields.
 */
object SevenSegmentReader {
    /** Height the display is straightened to, so every size rule below works in the same units. */
    const val DISPLAY_HEIGHT = 720

    /** Larger photos are shrunk to this first; it's plenty for the digits of a monitor. */
    private const val WORK_SIDE = 1600

    /** The display is found on a copy about this size. */
    private const val FIND_SIDE = 400

    /** How much of a segment's area must be dark for it to count as lit. */
    private const val LIT = 0.35f

    /** Lit segments, in the order a b c d e f g (top, top right, bottom right, bottom, bottom left, top left, middle). */
    private val DIGITS = mapOf(
        0b1111110 to 0, 0b0110000 to 1, 0b1101101 to 2, 0b1111001 to 3, 0b0110011 to 4,
        0b1011011 to 5, 0b1011111 to 6, 0b0011111 to 6, 0b1110000 to 7, 0b1110010 to 7,
        0b1111111 to 8, 0b1111011 to 9, 0b1110011 to 9,
    )

    /** Where each segment is sampled, as fractions of the digit's cell: left, right, top, bottom. */
    private val PROBES = arrayOf(
        floatArrayOf(0.3f, 0.7f, 0f, 0.12f),
        floatArrayOf(0.82f, 1f, 0.15f, 0.4f),
        floatArrayOf(0.82f, 1f, 0.6f, 0.85f),
        floatArrayOf(0.3f, 0.7f, 0.88f, 1f),
        floatArrayOf(0f, 0.18f, 0.6f, 0.85f),
        floatArrayOf(0f, 0.18f, 0.15f, 0.4f),
        floatArrayOf(0.3f, 0.7f, 0.44f, 0.56f),
    )

    fun read(image: GreyImage, displayHeight: Int = DISPLAY_HEIGHT): List<TextBox> {
        val work = image.fitWithin(WORK_SIDE)
        val whole = Quad.of(Box(0f, 0f, work.width.toFloat(), work.height.toFloat()))
        // A close-up or a tight crop has no bezel around the display, so the whole image is the fallback.
        val display = findDisplay(work)
        val fromDisplay = display?.let { readQuad(work, it, inset = 0.015f, displayHeight) }.orEmpty()
        val numbers = if (fromDisplay.size >= 2) {
            fromDisplay
        } else {
            val fromWhole = readQuad(work, whole, inset = 0f, displayHeight)
            if (fromWhole.size > fromDisplay.size) fromWhole else fromDisplay
        }
        val sx = image.width / work.width.toFloat()
        val sy = image.height / work.height.toFloat()
        return numbers.map { it.copy(bounds = Box(it.bounds.left * sx, it.bounds.top * sy, it.bounds.right * sx, it.bounds.bottom * sy)) }
    }

    /** Corners of the biggest bright, solid, roughly rectangular patch that doesn't touch the edge. */
    internal fun findDisplay(image: GreyImage): Quad? {
        val small = image.fitWithin(FIND_SIDE)
        val w = small.width
        val h = small.height
        val smooth = boxMean(FloatArray(w * h) { (small.pixels[it].toInt() and 0xFF).toFloat() }, w, h, 2)
        val cut = otsu(IntArray(w * h) { smooth[it].toInt() })
        val parts = Components(BooleanArray(w * h) { smooth[it] > cut }, w, h)
        var best = -1
        var bestScore = 0f
        for (i in 0 until parts.count) {
            val bw = parts.x1[i] - parts.x0[i] + 1
            val bh = parts.y1[i] - parts.y0[i] + 1
            if (parts.touchesEdge(i) || parts.area[i] < 0.02f * w * h) continue
            val fill = parts.area[i] / (bw * bh).toFloat()
            if (fill < 0.5f || bw / bh.toFloat() !in 0.4f..2.5f) continue
            if (parts.area[i] * fill > bestScore) {
                bestScore = parts.area[i] * fill
                best = i
            }
        }
        if (best < 0) return null
        // The corners are the patch's extremes along the two diagonals.
        val corners = arrayOfNulls<Point>(4)
        val extremes = floatArrayOf(Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE)
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (parts.labels[y * w + x] != best + 1) continue
                val sum = (x + y).toFloat()
                val diff = (x - y).toFloat()
                val p = Point((x + 0.5f) * image.width / w, (y + 0.5f) * image.height / h)
                if (sum < extremes[0]) { extremes[0] = sum; corners[0] = p }
                if (diff > extremes[1]) { extremes[1] = diff; corners[1] = p }
                if (sum > extremes[2]) { extremes[2] = sum; corners[2] = p }
                if (diff < extremes[3]) { extremes[3] = diff; corners[3] = p }
            }
        }
        return Quad(corners[0]!!, corners[1]!!, corners[2]!!, corners[3]!!)
    }

    private fun readQuad(image: GreyImage, quad: Quad, inset: Float, displayHeight: Int): List<TextBox> {
        val area = quad.inset(inset)
        val aspect = (area.topWidth + area.bottomWidth) / (area.leftHeight + area.rightHeight)
        val w = (displayHeight * aspect).roundToInt().coerceIn(displayHeight / 4, displayHeight * 3)
        val h = displayHeight
        val straight = FloatArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = area.map((x + 0.5f) / w, (y + 0.5f) / h)
                straight[y * w + x] = image.sample(p.x, p.y)
            }
        }
        val dark = clean(threshold(straight, w, h), w, h)
        return numbers(dark, w, h).map { (text, box, confidence) ->
            val corners = listOf(
                area.map(box.left / w, box.top / h), area.map(box.right / w, box.top / h),
                area.map(box.right / w, box.bottom / h), area.map(box.left / w, box.bottom / h),
            )
            TextBox(
                text = text,
                bounds = Box(corners.minOf { it.x }, corners.minOf { it.y }, corners.maxOf { it.x }, corners.maxOf { it.y }),
                confidence = confidence,
            )
        }
    }

    /** Dark segments: pixels clearly darker than their surroundings once the lighting is evened out. */
    private fun threshold(grey: FloatArray, w: Int, h: Int): BooleanArray {
        val background = boxMean(grey, w, h, h / 10)
        val ratio = FloatArray(w * h) { grey[it] / max(background[it], 1f) }
        val cut = (otsu(IntArray(w * h) { (ratio[it] * 200).toInt().coerceIn(0, 255) }) / 200f).coerceIn(0.6f, 0.9f)
        return BooleanArray(w * h) { ratio[it] < cut }
    }

    /** Drops the bezel's shadow along the edges and specks of noise. */
    private fun clean(dark: BooleanArray, w: Int, h: Int): BooleanArray {
        val parts = Components(dark, w, h)
        val minArea = 0.00005f * w * h
        val keep = BooleanArray(parts.count + 1)
        for (i in 0 until parts.count) keep[i + 1] = !parts.touchesEdge(i) && parts.area[i] > minArea
        return BooleanArray(w * h) { keep[parts.labels[it]] }
    }

    private data class Number(val text: String, val bounds: Box, val confidence: Float)

    private class Cell(val left: Int, val right: Int) {
        val width: Int get() = right - left + 1
    }

    private fun numbers(dark: BooleanArray, w: Int, h: Int): List<Number> {
        val rows = digitRows(dark, w, h)
        val cellsByRow = rows.map { (top, bottom) -> digitCells(dark, w, top, bottom) }
        // The usual digit shape, from rows whose widest cell looks like a single digit. It sizes the
        // cells of a row of only 1s, and splits digits that have run together in a small or blurred photo.
        val aspect = rows.indices.mapNotNull { i ->
            val widest = cellsByRow[i].maxOfOrNull { it.width } ?: return@mapNotNull null
            val height = rows[i].second - rows[i].first + 1
            (widest / height.toFloat()).takeIf { it in 0.3f..0.8f }
        }.sorted().let { if (it.isEmpty()) 0.55f else it[it.size / 2] }

        val found = mutableListOf<Number>()
        for ((index, row) in rows.withIndex()) {
            val (top, bottom) = row
            val height = bottom - top + 1
            val cells = cellsByRow[index].flatMap { split(dark, w, top, bottom, it, aspect * height) }
            if (cells.isEmpty()) continue
            val widest = cells.maxOf { it.width }
            val cellWidth = if (widest >= 0.3f * height) widest else (aspect * height).roundToInt()

            val digits = StringBuilder()
            var left = 0
            var lastRight = -1
            var confidence = 1f
            fun finish() {
                if (digits.length >= 2) {
                    found += Number(digits.toString(), Box(left.toFloat(), top.toFloat(), lastRight + 1f, bottom + 1f), confidence)
                }
                digits.clear()
                confidence = 1f
            }
            for (cell in cells) {
                var start = cell.left
                if (cell.width < 0.45f * cellWidth) {
                    // Only the right-hand segments are lit: a 1, right-aligned in a full-width cell.
                    // A narrow mark whose cell would overlap the digit before it is something else,
                    // such as the level bar beside some monitors' systolic reading.
                    start = cell.right - cellWidth + 1
                    if (lastRight >= 0 && start < lastRight - 0.1f * cellWidth) continue
                }
                start = max(start, 0)
                // Digits of one number sit about a quarter of a digit apart; a wider gap, like a
                // clock's colon, starts another.
                if (digits.isNotEmpty() && start - lastRight > 0.6f * cellWidth) finish()
                val read = decode(dark, w, start, cell.right, top, bottom)
                if (read == null) {
                    finish()
                    continue
                }
                if (digits.isEmpty()) left = start
                digits.append(read.first)
                confidence = min(confidence, read.second)
                lastRight = cell.right
            }
            finish()
        }
        return found
    }

    /**
     * Bands of rows holding digits. A 0, 1 or 7 has no middle segment, so a row of them comes apart
     * into top and bottom halves. A half is squat: its widest digit is about as wide as the band is
     * tall, where a whole digit is about half as wide; a band of only 1s is judged against the widest
     * digit anywhere. Two halves of similar height with a narrow gap are joined back up.
     */
    private fun digitRows(dark: BooleanArray, w: Int, h: Int): List<Pair<Int, Int>> {
        val profile = IntArray(h) { y -> (0 until w).count { dark[y * w + it] } }
        val bands = runs(profile, 2).flatMap { splitAtNecks(profile, it, h) }.filter { (a, b) -> b - a >= 0.03f * h }
        if (bands.isEmpty()) return emptyList()
        val widest = bands.map { (a, b) -> digitCells(dark, w, a, b).maxOfOrNull { it.width } ?: 0 }
        val digitWidth = widest.max()
        fun isHalf(i: Int): Boolean {
            val height = bands[i].second - bands[i].first + 1
            return if (widest[i] >= 0.3f * height) widest[i] >= 0.75f * height else height < 1.3f * digitWidth
        }

        val rows = mutableListOf<IntArray>()
        var previousIsHalf = false
        for ((i, band) in bands.withIndex()) {
            val half = isHalf(i)
            val previous = rows.lastOrNull()
            if (previous != null && previousIsHalf && half) {
                val ph = previous[1] - previous[0]
                val bh = band.second - band.first
                val tallest = max(ph, bh)
                if (band.first - previous[1] < 0.35f * tallest && abs(ph - bh) < 0.35f * tallest) {
                    previous[1] = band.second
                    previousIsHalf = false
                    continue
                }
            }
            rows += intArrayOf(band.first, band.second)
            previousIsHalf = half
        }
        return rows.map { it[0] to it[1] }
    }

    /**
     * Splits a band where it narrows to a thin neck for a few rows: a row of digits joined to an
     * icon or label below it by a thin line.
     */
    private fun splitAtNecks(profile: IntArray, band: Pair<Int, Int>, h: Int): List<Pair<Int, Int>> {
        val (top, bottom) = band
        val lit = (top..bottom).map { profile[it] }.filter { it > 0 }.sorted()
        val thin = 0.2f * lit[lit.size / 2]
        val minRun = max(3, (0.01f * h).roundToInt())
        val out = mutableListOf<Pair<Int, Int>>()
        var start = top
        var y = top
        while (y <= bottom) {
            if (profile[y] >= thin) {
                y++
                continue
            }
            var end = y
            while (end + 1 <= bottom && profile[end + 1] < thin) end++
            // Only a neck with thicker rows on both sides; thin tips at the band's ends stay.
            if (end - y + 1 >= minRun && y > start && end < bottom) {
                out += start to y - 1
                start = end + 1
            }
            y = end + 1
        }
        out += start to bottom
        return out
    }

    /** Splits a cell much wider than one digit at its emptiest columns: digits that have run together. */
    private fun split(dark: BooleanArray, w: Int, top: Int, bottom: Int, cell: Cell, digitWidth: Float): List<Cell> {
        val parts = (cell.width / (1.1f * digitWidth)).roundToInt()
        if (cell.width < 1.6f * digitWidth || parts < 2) return listOf(cell)
        val profile = IntArray(cell.width) { i -> (top..bottom).count { dark[it * w + cell.left + i] } }
        val reach = (0.25f * digitWidth).roundToInt()
        val out = mutableListOf<Cell>()
        var start = cell.left
        for (k in 1 until parts) {
            val nominal = cell.left + k * cell.width / parts
            val cut = (max(start + 1, nominal - reach)..min(cell.right - 1, nominal + reach))
                .minByOrNull { profile[it - cell.left] } ?: nominal
            out += Cell(start, cut - 1)
            start = cut + 1
        }
        out += Cell(start, cell.right)
        return out
    }

    /** Column runs in a row band that reach most of its height: digits, and marks as tall as them. */
    private fun digitCells(dark: BooleanArray, w: Int, top: Int, bottom: Int): List<Cell> {
        val height = bottom - top + 1
        val profile = IntArray(w) { x -> (top..bottom).count { dark[it * w + x] } }
        return runs(profile, 1).mapNotNull { (a, b) ->
            var first = -1
            var last = -1
            for (y in top..bottom) {
                if ((a..b).any { dark[y * w + it] }) {
                    if (first < 0) first = y
                    last = y
                }
            }
            Cell(a, b).takeIf { last - first >= 0.75f * height }
        }
    }

    /**
     * The digit in a cell and how sure the read is: 1 when every segment is clearly lit or clearly
     * dark, falling towards 0.6 as the most doubtful segment nears the line between them.
     */
    private fun decode(dark: BooleanArray, w: Int, x0: Int, x1: Int, y0: Int, y1: Int): Pair<Char, Float>? {
        val cw = x1 - x0 + 1
        val ch = y1 - y0 + 1
        var lit = 0
        var clarity = 1f
        for (probe in PROBES) {
            val left = x0 + (probe[0] * cw).toInt()
            val right = x0 + max((probe[0] * cw).toInt() + 1, (probe[1] * cw).toInt())
            val top = y0 + (probe[2] * ch).toInt()
            val bottom = y0 + max((probe[2] * ch).toInt() + 1, (probe[3] * ch).toInt())
            var count = 0
            for (y in top until min(bottom, y1 + 1)) for (x in left until min(right, x1 + 1)) if (dark[y * w + x]) count++
            val size = (min(bottom, y1 + 1) - top) * (min(right, x1 + 1) - left)
            val fraction = if (size > 0) count / size.toFloat() else 0f
            lit = lit shl 1 or if (fraction > LIT) 1 else 0
            clarity = min(clarity, if (fraction > LIT) min(1f, (fraction - LIT) / LIT) else (LIT - fraction) / LIT)
        }
        val digit = DIGITS[lit] ?: return null
        return ('0' + digit) to 0.6f + 0.4f * clarity
    }

    /** Index ranges where [profile] is at least [min]. */
    private fun runs(profile: IntArray, min: Int): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var start = -1
        for (i in profile.indices) {
            if (profile[i] >= min) {
                if (start < 0) start = i
            } else if (start >= 0) {
                out += start to i - 1
                start = -1
            }
        }
        if (start >= 0) out += start to profile.size - 1
        return out
    }

    /** Mean over a (2r + 1)² window, shrunk at the edges to the pixels inside. */
    private fun boxMean(values: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        val stride = w + 1
        val sum = DoubleArray(stride * (h + 1))
        for (y in 0 until h) {
            var row = 0.0
            for (x in 0 until w) {
                row += values[y * w + x]
                sum[(y + 1) * stride + x + 1] = sum[y * stride + x + 1] + row
            }
        }
        val out = FloatArray(w * h)
        for (y in 0 until h) {
            val y0 = max(0, y - r)
            val y1 = min(h, y + r + 1)
            for (x in 0 until w) {
                val x0 = max(0, x - r)
                val x1 = min(w, x + r + 1)
                val s = sum[y1 * stride + x1] - sum[y0 * stride + x1] - sum[y1 * stride + x0] + sum[y0 * stride + x0]
                out[y * w + x] = (s / ((x1 - x0) * (y1 - y0))).toFloat()
            }
        }
        return out
    }

    /** The level that best splits [values] (0–255) into a dark and a light group. */
    private fun otsu(values: IntArray): Int {
        val histogram = LongArray(256)
        for (v in values) histogram[v.coerceIn(0, 255)]++
        val total = values.size.toDouble()
        val totalSum = (0 until 256).sumOf { it * histogram[it].toDouble() }
        var below = 0.0
        var belowSum = 0.0
        var best = -1.0
        var level = 128
        for (i in 0 until 255) {
            below += histogram[i]
            belowSum += i * histogram[i].toDouble()
            val above = total - below
            if (below == 0.0 || above == 0.0) continue
            val spread = below * above * (belowSum / below - (totalSum - belowSum) / above).let { it * it }
            if (spread > best) {
                best = spread
                level = i
            }
        }
        return level
    }
}

internal data class Point(val x: Float, val y: Float)

/** A four-cornered area of an image: top left, top right, bottom right, bottom left. */
internal data class Quad(val topLeft: Point, val topRight: Point, val bottomRight: Point, val bottomLeft: Point) {
    val topWidth: Float get() = distance(topLeft, topRight)
    val bottomWidth: Float get() = distance(bottomLeft, bottomRight)
    val leftHeight: Float get() = distance(topLeft, bottomLeft)
    val rightHeight: Float get() = distance(topRight, bottomRight)

    // The perspective transform taking the unit square onto these corners (Heckbert's square-to-quad).
    private val g: Float
    private val h: Float
    private val a: Float
    private val b: Float
    private val d: Float
    private val e: Float

    init {
        val (x0, y0) = topLeft
        val (x1, y1) = topRight
        val (x2, y2) = bottomRight
        val (x3, y3) = bottomLeft
        val sx = x0 - x1 + x2 - x3
        val sy = y0 - y1 + y2 - y3
        val det = (x1 - x2) * (y3 - y2) - (x3 - x2) * (y1 - y2)
        if (abs(sx) < 1e-3f && abs(sy) < 1e-3f || abs(det) < 1e-6f) {
            g = 0f
            h = 0f
        } else {
            g = (sx * (y3 - y2) - (x3 - x2) * sy) / det
            h = ((x1 - x2) * sy - sx * (y1 - y2)) / det
        }
        a = x1 - x0 + g * x1
        b = x3 - x0 + h * x3
        d = y1 - y0 + g * y1
        e = y3 - y0 + h * y3
    }

    /** The point at ([u], [v]) across and down the area, each from 0 to 1. */
    fun map(u: Float, v: Float): Point {
        val z = g * u + h * v + 1
        return Point((a * u + b * v + topLeft.x) / z, (d * u + e * v + topLeft.y) / z)
    }

    /** Moves each corner [fraction] of the way towards the opposite one. */
    fun inset(fraction: Float): Quad {
        if (fraction == 0f) return this
        fun towards(p: Point, q: Point) = Point(p.x + (q.x - p.x) * fraction, p.y + (q.y - p.y) * fraction)
        return Quad(towards(topLeft, bottomRight), towards(topRight, bottomLeft), towards(bottomRight, topLeft), towards(bottomLeft, topRight))
    }

    companion object {
        fun of(box: Box) = Quad(
            Point(box.left, box.top), Point(box.right, box.top),
            Point(box.right, box.bottom), Point(box.left, box.bottom),
        )

        private fun distance(p: Point, q: Point) = hypot(p.x - q.x, p.y - q.y)
    }
}

/** 4-connected regions of a mask, labelled from 1, with each one's area and bounds. */
private class Components(mask: BooleanArray, private val w: Int, private val h: Int) {
    val labels = IntArray(w * h)
    var count = 0
        private set
    var area = IntArray(16)
        private set
    var x0 = IntArray(16)
        private set
    var y0 = IntArray(16)
        private set
    var x1 = IntArray(16)
        private set
    var y1 = IntArray(16)
        private set

    init {
        val queue = IntArray(w * h)
        for (start in mask.indices) {
            if (!mask[start] || labels[start] != 0) continue
            if (count == area.size) grow()
            val label = count + 1
            var head = 0
            var tail = 0
            queue[tail++] = start
            labels[start] = label
            var minX = w
            var minY = h
            var maxX = -1
            var maxY = -1
            while (head < tail) {
                val p = queue[head++]
                val x = p % w
                val y = p / w
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (x > 0 && mask[p - 1] && labels[p - 1] == 0) { labels[p - 1] = label; queue[tail++] = p - 1 }
                if (x < w - 1 && mask[p + 1] && labels[p + 1] == 0) { labels[p + 1] = label; queue[tail++] = p + 1 }
                if (y > 0 && mask[p - w] && labels[p - w] == 0) { labels[p - w] = label; queue[tail++] = p - w }
                if (y < h - 1 && mask[p + w] && labels[p + w] == 0) { labels[p + w] = label; queue[tail++] = p + w }
            }
            area[count] = tail
            x0[count] = minX
            y0[count] = minY
            x1[count] = maxX
            y1[count] = maxY
            count++
        }
    }

    fun touchesEdge(i: Int) = x0[i] == 0 || y0[i] == 0 || x1[i] == w - 1 || y1[i] == h - 1

    private fun grow() {
        area = area.copyOf(area.size * 2)
        x0 = x0.copyOf(x0.size * 2)
        y0 = y0.copyOf(y0.size * 2)
        x1 = x1.copyOf(x1.size * 2)
        y1 = y1.copyOf(y1.size * 2)
    }
}

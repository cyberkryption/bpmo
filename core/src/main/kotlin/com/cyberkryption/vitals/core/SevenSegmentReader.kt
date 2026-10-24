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
 *
 * A screen lit the other way, light digits on black like most oximeters', is read the same way
 * from an inverted copy, and also as digits in an ordinary typeface, each read from its shape.
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
        var (numbers, best) = readDisplays(work, displayHeight, Screen.LCD)
        // Not even two values: perhaps a screen lit the other way, like an oximeter's.
        if (best.fields < 2) {
            val (light, score) = readDisplays(work.inverted(), displayHeight, Screen.LIT)
            if (score > best) numbers = light
        }
        val sx = image.width / work.width.toFloat()
        val sy = image.height / work.height.toFloat()
        return numbers.map { it.copy(bounds = Box(it.bounds.left * sx, it.bounds.top * sy, it.bounds.right * sx, it.bounds.bottom * sy)) }
    }

    /** How the digits are drawn: as seven segments, or as solid shapes in an ordinary typeface. */
    private enum class Glyphs { SEGMENTS, SHAPES }

    /** The kinds of screen, by how they show their digits, as the reader sees them: dark on light. */
    private enum class Screen(val glyphs: List<Glyphs>) {
        /** Dark segments on a grey panel lit by the room, so a shadow can fall across it. */
        LCD(listOf(Glyphs.SEGMENTS)),

        /**
         * Light digits on black that make their own light, like an oximeter's OLED screen, in
         * segments or an ordinary typeface. The reader sees them inverted.
         */
        LIT(listOf(Glyphs.SEGMENTS, Glyphs.SHAPES)),
    }

    /**
     * The numbers on the likeliest display areas in [work], best first: whichever area gives the
     * most values wins, and a full set of blood pressure values ends the search.
     */
    private fun readDisplays(work: GreyImage, displayHeight: Int, screen: Screen): Pair<List<TextBox>, Score> {
        var numbers = emptyList<TextBox>()
        var best = Score(-1, -1)
        for (quad in findDisplays(work)) {
            val read = readQuad(work, quad, inset = 0.015f, displayHeight, screen)
            val score = score(read)
            if (score > best) {
                numbers = read
                best = score
            }
            if (best.fields == ReadingType.BP.fields.size) break
        }
        // A close-up or a tight crop has no bezel around the display, so the whole image is the fallback.
        if (best.fields < 2) {
            val whole = Quad.of(Box(0f, 0f, work.width.toFloat(), work.height.toFloat()))
            val read = readQuad(work, whole, inset = 0f, displayHeight, screen)
            val score = score(read)
            if (score > best) {
                numbers = read
                best = score
            }
        }
        return numbers to best
    }

    /** How good a read is: the most fields it fills as either monitor type, then how many numbers it found. */
    private data class Score(val fields: Int, val numbers: Int) : Comparable<Score> {
        override fun compareTo(other: Score) = compareValuesBy(this, other, Score::fields, Score::numbers)
    }

    private fun score(numbers: List<TextBox>) = Score(
        ReadingType.entries.maxOf { DisplayParser.parse(numbers, it).detections.size },
        numbers.size,
    )

    /**
     * Where the display most likely is: up to three bright, solid, roughly rectangular patches that
     * don't touch the edge, best first.
     *
     * - Bright bridges, such as a reflection on the bezel joining the display to a bright wall,
     *   are broken first, gently and then harder.
     * - A display lit unevenly can be dim at one end, so each patch is tried as found and also
     *   grown into neighbouring pixels too dim to count as bright but well clear of the dark bezel.
     * - Corners are where lines fitted along the four sides meet, so rounded corners, icons and
     *   reflections don't skew the display when it's straightened.
     */
    internal fun findDisplays(image: GreyImage): List<Quad> {
        val small = image.fitWithin(FIND_SIDE)
        val w = small.width
        val h = small.height
        val smooth = boxMean(FloatArray(w * h) { (small.pixels[it].toInt() and 0xFF).toFloat() }, w, h, 2)
        val cut = otsu(IntArray(w * h) { smooth[it].toInt() })
        val bright = BooleanArray(w * h) { smooth[it] > cut }
        val darkMean = smooth.filter { it <= cut }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        // Dimmer than the display's bright part, but brighter than the bezel. Bright pixels are
        // left out so growing can't cross a reflection into a bright wall.
        val dim = BooleanArray(w * h) { smooth[it] > (darkMean + cut) / 2 && smooth[it] <= cut }
        val sx = image.width / w.toFloat()
        val sy = image.height / h.toFloat()
        val found = mutableListOf<Pair<Float, Box>>()
        val quads = mutableListOf<List<Quad>>()
        for (radius in listOf(max(2, max(w, h) / 130), max(4, max(w, h) / 50))) {
            val parts = Components(open(bright, w, h, radius), w, h)
            for (i in 0 until parts.count) {
                val bw = parts.x1[i] - parts.x0[i] + 1
                val bh = parts.y1[i] - parts.y0[i] + 1
                val fill = parts.area[i] / (bw * bh).toFloat()
                if (parts.touchesEdge(i) || parts.area[i] < 0.02f * w * h || fill < 0.5f || bw / bh.toFloat() !in 0.4f..2.5f) continue
                val box = Box(parts.x0[i].toFloat(), parts.y0[i].toFloat(), parts.x1[i] + 1f, parts.y1[i] + 1f)
                // The same patch found again after the harder opening is a duplicate.
                if (found.any { overlap(it.second, box) > 0.6f }) continue
                found += parts.area[i] * fill to box
                val patch = BooleanArray(w * h) { parts.labels[it] == i + 1 }
                val grown = grow(parts, i + 1, dim, w, h)
                // The patch as found, then grown if that added much; reading decides between them.
                val shapes = if (grown.count { it } > 1.1f * parts.area[i]) listOf(patch, grown) else listOf(patch)
                quads += shapes.map { region ->
                    val q = fitQuad(region, w, h)
                    Quad(q.topLeft.scaled(sx, sy), q.topRight.scaled(sx, sy), q.bottomRight.scaled(sx, sy), q.bottomLeft.scaled(sx, sy))
                }
            }
        }
        return found.indices.sortedByDescending { found[it].first }.take(3).flatMap { quads[it] }
    }

    /** Shared area over the smaller box's area. */
    private fun overlap(a: Box, b: Box): Float {
        val w = min(a.right, b.right) - max(a.left, b.left)
        val h = min(a.bottom, b.bottom) - max(a.top, b.top)
        if (w <= 0 || h <= 0) return 0f
        return w * h / min(a.width * a.height, b.width * b.height)
    }

    /** Opening: shrinks a mask by [r] pixels and grows it back, dropping anything thinner than that. */
    private fun open(mask: BooleanArray, w: Int, h: Int, r: Int): BooleanArray {
        fun counts(m: BooleanArray) = boxMean(FloatArray(w * h) { if (m[it]) 1f else 0f }, w, h, r)
        val shrunk = counts(mask).let { mean -> BooleanArray(w * h) { mean[it] > 0.999f } }
        return counts(shrunk).let { mean -> BooleanArray(w * h) { mean[it] > 0.001f } }
    }

    /** The patch labelled [label], grown into connected [dim] pixels no further than a third of its size beyond it. */
    private fun grow(parts: Components, label: Int, dim: BooleanArray, w: Int, h: Int): BooleanArray {
        val i = label - 1
        val reachX = (parts.x1[i] - parts.x0[i] + 1) / 3
        val reachY = (parts.y1[i] - parts.y0[i] + 1) / 3
        val minX = max(0, parts.x0[i] - reachX)
        val maxX = min(w - 1, parts.x1[i] + reachX)
        val minY = max(0, parts.y0[i] - reachY)
        val maxY = min(h - 1, parts.y1[i] + reachY)
        val region = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var tail = 0
        for (p in region.indices) {
            if (parts.labels[p] == label) {
                region[p] = true
                queue[tail++] = p
            }
        }
        var head = 0
        while (head < tail) {
            val p = queue[head++]
            val x = p % w
            val y = p / w
            for (n in intArrayOf(if (x > minX) p - 1 else -1, if (x < maxX) p + 1 else -1, if (y > minY) p - w else -1, if (y < maxY) p + w else -1)) {
                if (n >= 0 && !region[n] && dim[n]) {
                    region[n] = true
                    queue[tail++] = n
                }
            }
        }
        return region
    }

    /**
     * The quad where lines fitted to the region's four sides meet. Each side is sampled along its
     * middle 70%, away from rounded corners. Falls back to the region's extreme points if a side
     * can't be fitted.
     */
    private fun fitQuad(region: BooleanArray, w: Int, h: Int): Quad {
        var x0 = w
        var y0 = h
        var x1 = -1
        var y1 = -1
        for (p in region.indices) {
            if (!region[p]) continue
            val x = p % w
            val y = p / w
            if (x < x0) x0 = x
            if (x > x1) x1 = x
            if (y < y0) y0 = y
            if (y > y1) y1 = y
        }
        fun at(x: Int, y: Int) = region[y * w + x]
        val xs = middle(x0, x1)
        val ys = middle(y0, y1)
        val top = xs.mapNotNull { x -> (y0..y1).firstOrNull { at(x, it) }?.let { x.toFloat() to it.toFloat() } }
        val bottom = xs.mapNotNull { x -> (y1 downTo y0).firstOrNull { at(x, it) }?.let { x.toFloat() to it + 1f } }
        val left = ys.mapNotNull { y -> (x0..x1).firstOrNull { at(it, y) }?.let { y.toFloat() to it.toFloat() } }
        val right = ys.mapNotNull { y -> (x1 downTo x0).firstOrNull { at(it, y) }?.let { y.toFloat() to it + 1f } }
        val t = fitEdge(top)
        val b = fitEdge(bottom)
        val l = fitEdge(left)
        val r = fitEdge(right)
        if (t == null || b == null || l == null || r == null) return extremes(region, w, x0, y0, x1, y1)
        // y = t.a + t.b·x meets x = l.a + l.b·y
        fun meet(row: Pair<Float, Float>, col: Pair<Float, Float>): Point {
            val x = (col.first + col.second * row.first) / (1 - row.second * col.second)
            return Point(x, row.first + row.second * x)
        }
        return Quad(meet(t, l), meet(t, r), meet(b, r), meet(b, l))
    }

    private fun middle(from: Int, to: Int): IntRange {
        val margin = ((to - from) * 0.15f).roundToInt()
        return (from + margin)..(to - margin)
    }

    /**
     * The line v = a + b·u along one side of a patch, from its (u, v) edge points: the straight
     * line the most points lie on, refitted to them. Icons and digits cut into a side and
     * reflections bulge out of it, but only along part of it. Null with too few points.
     */
    private fun fitEdge(points: List<Pair<Float, Float>>): Pair<Float, Float>? {
        if (points.size < 5) return null
        val step = max(1, points.size / 40)
        val sample = points.filterIndexed { k, _ -> k % step == 0 }
        val span = points.last().first - points.first().first
        var best: Pair<Float, Float>? = null
        var bestCount = -1
        for (i in sample.indices) {
            for (j in i + 1 until sample.size) {
                val (u1, v1) = sample[i]
                val (u2, v2) = sample[j]
                if (abs(u2 - u1) < 0.2f * abs(span)) continue
                val slope = (v2 - v1) / (u2 - u1)
                if (abs(slope) > 1f) continue
                val line = v1 - slope * u1 to slope
                val count = points.count { abs(it.second - (line.first + line.second * it.first)) <= 1.5f }
                if (count > bestCount) {
                    bestCount = count
                    best = line
                }
            }
        }
        val line = best ?: return null
        val inliers = points.filter { abs(it.second - (line.first + line.second * it.first)) <= 1.5f }
        return leastSquares(inliers) ?: line
    }

    private fun leastSquares(points: List<Pair<Float, Float>>): Pair<Float, Float>? {
        if (points.size < 2) return null
        val mu = points.sumOf { it.first.toDouble() } / points.size
        val mv = points.sumOf { it.second.toDouble() } / points.size
        val suu = points.sumOf { (it.first - mu) * (it.first - mu) }
        if (suu == 0.0) return null
        val slope = points.sumOf { (it.first - mu) * (it.second - mv) } / suu
        return (mv - slope * mu).toFloat() to slope.toFloat()
    }

    /** The region's extremes along the two diagonals, as corners. */
    private fun extremes(region: BooleanArray, w: Int, x0: Int, y0: Int, x1: Int, y1: Int): Quad {
        val corners = arrayOfNulls<Point>(4)
        val best = floatArrayOf(Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE)
        for (y in y0..y1) {
            for (x in x0..x1) {
                if (!region[y * w + x]) continue
                val sum = (x + y).toFloat()
                val diff = (x - y).toFloat()
                val p = Point(x + 0.5f, y + 0.5f)
                if (sum < best[0]) { best[0] = sum; corners[0] = p }
                if (diff > best[1]) { best[1] = diff; corners[1] = p }
                if (sum > best[2]) { best[2] = sum; corners[2] = p }
                if (diff < best[3]) { best[3] = diff; corners[3] = p }
            }
        }
        return Quad(corners[0]!!, corners[1]!!, corners[2]!!, corners[3]!!)
    }

    /** The numbers in [quad], read as each kind of digit the [screen] may show, keeping the best read. */
    private fun readQuad(image: GreyImage, quad: Quad, inset: Float, displayHeight: Int, screen: Screen): List<TextBox> {
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
        val dark = clean(threshold(straight, w, h, screen), w, h)
        return screen.glyphs.map { style ->
            val found = when (style) {
                Glyphs.SEGMENTS -> numbers(dark, w, h)
                Glyphs.SHAPES -> shapeNumbers(dark, w, h)
            }
            found.map { (text, box, confidence) ->
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
        }.maxBy(::score)
    }

    /**
     * Dark segments: pixels clearly darker than their surroundings once the lighting is evened out.
     * The surroundings are the local mean, checked against the local brightness with thin strokes
     * filled in, which keeps edges sharp:
     * - An LCD lit by the room can be half in shadow. The darker of the two stops the shadowed side
     *   being judged against the lit side.
     * - A lit screen is evenly black right up to its digits, so evenly light as the reader sees it.
     *   The lighter of the two stops a dark case around it pulling the screen's level down.
     */
    private fun threshold(grey: FloatArray, w: Int, h: Int, screen: Screen): BooleanArray {
        val background = boxMean(grey, w, h, h / 10)
        val sharp = closing(grey, w, h, h / 20)
        for (i in background.indices) {
            background[i] = when (screen) {
                Screen.LCD -> min(background[i], sharp[i])
                Screen.LIT -> max(background[i], sharp[i])
            }
        }
        val ratio = FloatArray(w * h) { grey[it] / max(background[it], 1f) }
        val cut = (otsu(IntArray(w * h) { (ratio[it] * 200).toInt().coerceIn(0, 255) }) / 200f).coerceIn(0.6f, 0.9f)
        return BooleanArray(w * h) { ratio[it] < cut }
    }

    /**
     * Drops the bezel's shadow along the edges, specks of noise, solid blocks such as a level bar's
     * squares, which can sit between two rows of digits and join them, and outlines around the
     * digits, such as a screen's frame, which join every row. Segments are long and thin, so they stay.
     */
    private fun clean(dark: BooleanArray, w: Int, h: Int): BooleanArray {
        val parts = Components(dark, w, h)
        val minArea = 0.00005f * w * h
        val keep = BooleanArray(parts.count + 1)
        for (i in 0 until parts.count) {
            val bw = parts.x1[i] - parts.x0[i] + 1
            val bh = parts.y1[i] - parts.y0[i] + 1
            val block = parts.area[i] > 0.8f * bw * bh && max(bw, bh) < 1.8f * min(bw, bh)
            // Big but mostly empty: no digit is both.
            val outline = bw > 0.3f * w && bh > 0.3f * h && parts.area[i] < 0.15f * bw * bh
            keep[i + 1] = !parts.touchesEdge(i) && parts.area[i] > minArea && !block && !outline
        }
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
            val (bandTop, bandBottom) = row
            val height = bandBottom - bandTop + 1
            val cells = cellsByRow[index].flatMap { split(dark, w, bandTop, bandBottom, it, aspect * height) }
            if (cells.isEmpty()) continue
            val widest = cells.maxOf { it.width }
            val cellWidth = if (widest >= 0.3f * height) widest else (aspect * height).roundToInt()
            // The digits' own top and bottom, from the full-width ones: a mark reaching into the
            // band, such as a level bar's square, doesn't stretch them.
            val extents = cells.filter { it.width >= 0.45f * cellWidth }.map { extent(dark, w, it, bandTop, bandBottom) }
            val top = extents.minOfOrNull { it.first } ?: bandTop
            val bottom = extents.maxOfOrNull { it.second } ?: bandBottom

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
        val bands = runs(profile, 2)
            .flatMap { splitBesideMarks(dark, w, it) }
            .flatMap { splitAtNecks(profile, it, h) }
            .filter { (a, b) -> b - a >= 0.03f * h }
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
     * Splits a band that two rows of digits share only through a narrow mark beside them, such as
     * a level bar's square lying across the gap: the band's rows are found again without the
     * columns of narrow marks wholly outside its full-width digits.
     */
    private fun splitBesideMarks(dark: BooleanArray, w: Int, band: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (top, bottom) = band
        val columns = runs(IntArray(w) { x -> (top..bottom).count { dark[it * w + x] } }, 1)
        if (columns.size < 2) return listOf(band)
        val widest = columns.maxOf { it.second - it.first + 1 }
        val wide = columns.filter { it.second - it.first + 1 >= 0.45f * widest }
        val from = wide.minOf { it.first }
        val to = wide.maxOf { it.second }
        val beside = columns.filter { it.second < from || it.first > to }
        if (beside.isEmpty()) return listOf(band)
        val skip = BooleanArray(w)
        for ((a, b) in beside) for (x in a..b) skip[x] = true
        val profile = IntArray(bottom - top + 1) { i -> (0 until w).count { x -> !skip[x] && dark[(top + i) * w + x] } }
        return runs(profile, 2).map { (a, b) -> top + a to top + b }
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

    /** The first and last rows of the band with anything dark in the cell's columns. */
    private fun extent(dark: BooleanArray, w: Int, cell: Cell, top: Int, bottom: Int): Pair<Int, Int> {
        var first = -1
        var last = -1
        for (y in top..bottom) {
            if ((cell.left..cell.right).any { dark[y * w + it] }) {
                if (first < 0) first = y
                last = y
            }
        }
        return if (first < 0) top to bottom else first to last
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

    /**
     * Numbers in an ordinary typeface, where each digit is one solid shape. Each shape tall enough
     * to be a digit is read on its own, split first if it's as wide as two run together, and
     * digits side by side at the same height make up a number. A screen's frame, icons and graphs
     * read as no digit, so unlike rows of segments they can't join anything up.
     */
    private fun shapeNumbers(dark: BooleanArray, w: Int, h: Int): List<Number> {
        val parts = Components(dark, w, h)
        val digits = mutableListOf<Number>()
        val specks = mutableListOf<Box>()
        for (i in 0 until parts.count) {
            val bw = parts.x1[i] - parts.x0[i] + 1
            val bh = parts.y1[i] - parts.y0[i] + 1
            val box = Box(parts.x0[i].toFloat(), parts.y0[i].toFloat(), parts.x1[i] + 1f, parts.y1[i] + 1f)
            if (bh < MIN_SHAPE * h) {
                specks += box
                continue
            }
            if (bw > 3 * bh) continue
            val own = BooleanArray(bw * bh) { parts.labels[(parts.y0[i] + it / bw) * w + parts.x0[i] + it % bw] == i + 1 }
            val pieces = if (bw > 0.9f * bh) (bw / (0.7f * bh)).roundToInt() else 1
            val cuts = splitColumns(own, bw, bh, pieces)
            val read = (0 until cuts.size - 1).map { k ->
                shape(own, bw, cuts[k], cuts[k + 1] - 1, 0, bh - 1)?.let { (digit, confidence) ->
                    Number(digit.toString(), Box(box.left + cuts[k], box.top, box.left + cuts[k + 1], box.bottom), confidence)
                }
            }
            // Digits run together split into whole digits; a letter like m splits into strokes that
            // pass for 1s, which are too narrow to run into anything.
            if (pieces == 1 || read.all { it != null && it.text != "1" }) digits += read.filterNotNull()
        }
        val numbers = mutableListOf<MutableList<Number>>()
        for (digit in digits.sortedBy { it.bounds.left }) {
            val b = digit.bounds
            val number = numbers.lastOrNull { n ->
                val last = n.last().bounds
                val gap = b.left - last.right
                abs(b.height - last.height) < 0.15f * last.height &&
                    abs(b.centerY - last.centerY) < 0.15f * last.height &&
                    gap in -0.1f * last.height..0.35f * last.height &&
                    // A decimal point between them parts two numbers, like PI 4.7.
                    specks.none { it.left >= last.right - 1 && it.right <= b.left + 1 && it.top > last.top + 0.6f * last.height && it.bottom <= last.bottom + 0.1f * last.height }
            }
            if (number != null) number += digit else numbers += mutableListOf(digit)
        }
        return numbers.filter { it.size >= 2 }.map { n ->
            Number(
                n.joinToString("") { it.text },
                Box(n.minOf { it.bounds.left }, n.minOf { it.bounds.top }, n.maxOf { it.bounds.right }, n.maxOf { it.bounds.bottom }),
                n.minOf { it.confidence },
            )
        }
    }

    /** Column boundaries splitting a shape into [pieces] at its emptiest columns near even spacing, from 0 to [w]. */
    private fun splitColumns(shape: BooleanArray, w: Int, h: Int, pieces: Int): IntArray {
        if (pieces < 2) return intArrayOf(0, w)
        val profile = IntArray(w) { x -> (0 until h).count { shape[it * w + x] } }
        val reach = w / pieces / 4
        val cuts = IntArray(pieces + 1)
        cuts[pieces] = w
        for (k in 1 until pieces) {
            val nominal = k * w / pieces
            cuts[k] = (max(cuts[k - 1] + 1, nominal - reach)..min(w - 1, nominal + reach)).minByOrNull { profile[it] } ?: nominal
        }
        return cuts
    }

    /** Shapes shorter than this share of the display are specks, points and small print, not digits. */
    private const val MIN_SHAPE = 0.022f

    /**
     * The digit a glyph in an ordinary typeface shows, from what every typeface shares: its holes
     * and the sides it's open on. A 6 has a hole low down and is open at the top right; a 2 is
     * open at the top left and the bottom right above a flat foot; and so on.
     */
    private fun shape(dark: BooleanArray, w: Int, x0: Int, x1: Int, y0: Int, y1: Int): Pair<Char, Float>? {
        var gx0 = x1
        var gx1 = x0
        var gy0 = y1
        var gy1 = y0
        for (y in y0..y1) {
            for (x in x0..x1) {
                if (!dark[y * w + x]) continue
                if (x < gx0) gx0 = x
                if (x > gx1) gx1 = x
                if (y < gy0) gy0 = y
                if (y > gy1) gy1 = y
            }
        }
        if (gx1 < gx0) return null
        val gw = gx1 - gx0 + 1
        val gh = gy1 - gy0 + 1
        val glyph = BooleanArray(gw * gh) { dark[(gy0 + it / gw) * w + gx0 + it % gw] }
        val background = Components(BooleanArray(gw * gh) { !glyph[it] }, gw, gh)
        val holes = (0 until background.count)
            .filter { !background.touchesEdge(it) && background.area[it] > 0.01f * gw * gh }
            .map { i -> Box(background.x0[i] / gw.toFloat(), background.y0[i] / gh.toFloat(), (background.x1[i] + 1) / gw.toFloat(), (background.y1[i] + 1) / gh.toFloat()) }
            .sortedBy { it.top }
        val fromLeft = FloatArray(gh) { y -> ((0 until gw).firstOrNull { glyph[y * gw + it] } ?: gw) / gw.toFloat() }
        val fromRight = FloatArray(gh) { y -> ((0 until gw).firstOrNull { glyph[y * gw + gw - 1 - it] } ?: gw) / gw.toFloat() }
        val width = FloatArray(gh) { 1f - fromLeft[it] - fromRight[it] }
        fun median(values: FloatArray, from: Float, to: Float): Float {
            val a = (from * values.size).toInt().coerceIn(0, values.size - 1)
            val b = (to * values.size).toInt().coerceIn(a + 1, values.size)
            return values.copyOfRange(a, b).sorted().let { it[it.size / 2] }
        }
        val aspect = gw / gh.toFloat()
        // The share of the height its longest column of ink covers: a 1's stem covers all of it.
        val stem = (0 until gw).maxOf { x -> (0 until gh).count { glyph[it * gw + x] } } / gh.toFloat()
        // How far in from the left (l…) and right (r…) the ink starts, as a share of the width, at
        // the top (…t), upper middle (…u), middle (…m), lower middle (…l) and bottom (…b).
        val lt = median(fromLeft, 0f, 0.12f)
        val lu = median(fromLeft, 0.25f, 0.42f)
        val lm = median(fromLeft, 0.42f, 0.58f)
        val ll = median(fromLeft, 0.58f, 0.75f)
        val lb = median(fromLeft, 0.88f, 1f)
        val rt = median(fromRight, 0f, 0.12f)
        val ru = median(fromRight, 0.25f, 0.42f)
        val rm = median(fromRight, 0.42f, 0.58f)
        val rl = median(fromRight, 0.58f, 0.75f)
        // How much of the width the ink spans at the top, middle and bottom.
        val top = median(width, 0f, 0.1f)
        val mid = median(width, 0.4f, 0.6f)
        val bottom = median(width, 0.9f, 1f)
        // Each candidate digit with the margins by which its tests pass; positive passes.
        val rules: List<Pair<Char, List<Float>>> = when (holes.size) {
            0 -> listOf(
                '1' to listOf(0.3f - aspect),
                '7' to listOf(top - 0.7f, 0.5f - bottom, lu - 0.3f, ll - 0.2f),
                '2' to listOf(bottom - 0.85f, 0.2f - ru, rl - 0.2f),
                '1' to listOf(stem - 0.85f, 0.6f - mid, 0.1f - abs(lm - ll), 0.1f - abs(rm - rl)),
                '3' to listOf(lu - ru, ll - 0.45f, 0.15f - rl, bottom - 0.45f),
                '5' to listOf(ru - lu - 0.15f, ll - 0.45f, 0.15f - rl),
                '4' to listOf(0.5f - top, 0.5f - bottom, mid - 0.55f, 0.15f - ll, lt - 0.3f, lb - 0.4f),
            )
            1 -> holes[0].let { hole ->
                listOf(
                    '0' to listOf(0.3f - hole.top, hole.bottom - 0.7f),
                    '6' to listOf(hole.centerY - 0.52f, ru - 0.15f, 0.25f - lu, 0.45f - rt),
                    '9' to listOf(0.52f - hole.centerY, 0.15f - lu, ll - 0.15f, 0.25f - rl),
                    '4' to listOf(0.52f - hole.centerY, lu - 0.15f, 0.5f - top, 0.8f - bottom, lt - 0.3f),
                )
            }
            2 -> listOf(
                '8' to listOf(
                    0.5f - holes[0].centerY, holes[1].centerY - 0.5f,
                    0.2f - abs(holes[0].centerX - 0.5f), 0.2f - abs(holes[1].centerX - 0.5f),
                ),
            )
            else -> emptyList()
        }
        val (digit, margins) = rules.firstOrNull { (_, m) -> m.all { it > 0 } } ?: return null
        // A digit that only scrapes through is no read: shapes around a screen, like a cable or
        // a key, can pass every test, but rarely clearly.
        if (margins.min() < SHAPE_MARGIN / 4) return null
        return digit to 0.6f + 0.4f * min(1f, margins.min() / SHAPE_MARGIN)
    }

    /** A shape test passing by this much is clear. */
    private const val SHAPE_MARGIN = 0.15f

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

    /**
     * The background behind dark strokes up to 2[r] wide: the brightest value within [r] of each
     * pixel, then the darkest of those within [r]. Unlike a mean it keeps a shadow's edge sharp.
     */
    private fun closing(values: FloatArray, w: Int, h: Int, r: Int): FloatArray =
        extreme(extreme(values, w, h, r, largest = true), w, h, r, largest = false)

    /** The largest (or smallest) value within a (2r + 1)² window, one row then one column at a time. */
    private fun extreme(values: FloatArray, w: Int, h: Int, r: Int, largest: Boolean): FloatArray {
        fun beats(a: Float, b: Float) = if (largest) a >= b else a <= b
        val rows = FloatArray(w * h)
        val queue = IntArray(max(w, h))
        fun line(from: FloatArray, to: FloatArray, start: Int, step: Int, n: Int) {
            var head = 0
            var tail = 0
            var next = 0
            for (i in 0 until n) {
                while (next < n && next <= i + r) {
                    val v = from[start + next * step]
                    while (tail > head && beats(v, from[start + queue[tail - 1] * step])) tail--
                    queue[tail++] = next++
                }
                while (queue[head] < i - r) head++
                to[start + i * step] = from[start + queue[head] * step]
            }
        }
        for (y in 0 until h) line(values, rows, y * w, 1, w)
        val out = FloatArray(w * h)
        for (x in 0 until w) line(rows, out, x, w, h)
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

internal data class Point(val x: Float, val y: Float) {
    fun scaled(sx: Float, sy: Float) = Point(x * sx, y * sy)
}

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

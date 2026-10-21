package com.cyberkryption.vitals.core

/**
 * Agreement across live camera frames. One frame can misread a digit, so values count only once
 * [needed] of the last [window] frames read exactly the same; a frame that read nothing complete
 * still takes up its place in the window.
 */
class FrameVotes(private val window: Int = 5, private val needed: Int = 3) {
    init {
        require(needed in 1..window)
    }

    private val recent = ArrayDeque<Map<Field, Int>?>()

    /** Adds one frame's values, null if it read nothing complete, and returns the agreed values if any. */
    fun add(values: Map<Field, Int>?): Map<Field, Int>? {
        recent.addLast(values)
        if (recent.size > window) recent.removeFirst()
        return agreed()
    }

    fun agreed(): Map<Field, Int>? =
        recent.filterNotNull().groupingBy { it }.eachCount().entries.firstOrNull { it.value >= needed }?.key

    fun clear() = recent.clear()
}

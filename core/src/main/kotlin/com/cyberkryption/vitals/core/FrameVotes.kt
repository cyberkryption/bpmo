package com.cyberkryption.vitals.core

/**
 * Agreement across live camera frames. One frame can misread a digit, so a value counts only once
 * [needed] of the last [window] frames read exactly it. Each value is agreed on its own, so frames
 * that each miss a different one, as a reflection moves across the display, still add up to the
 * whole reading.
 */
class FrameVotes(private val window: Int = 5, private val needed: Int = 3) {
    init {
        require(needed in 1..window)
    }

    private val recent = ArrayDeque<Map<Field, Int>>()

    /** Adds one frame's values, empty if it read none, and returns the values agreed so far. */
    fun add(values: Map<Field, Int>): Map<Field, Int> {
        recent.addLast(values)
        if (recent.size > window) recent.removeFirst()
        return agreed()
    }

    fun agreed(): Map<Field, Int> = recent.flatMap { it.entries }
        .groupingBy { it.key to it.value }
        .eachCount()
        .filterValues { it >= needed }
        .entries
        .groupBy({ it.key.first }, { it.key.second to it.value })
        .mapValues { (_, votes) -> votes.maxBy { it.second }.first }

    fun clear() = recent.clear()
}

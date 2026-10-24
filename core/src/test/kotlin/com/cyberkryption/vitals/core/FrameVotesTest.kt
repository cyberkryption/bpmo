package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrameVotesTest {
    private val good = mapOf(Field.SYSTOLIC to 154, Field.DIASTOLIC to 100, Field.PULSE to 76)
    private val misread = good + (Field.SYSTOLIC to 184)

    @Test
    fun agreesOnceEnoughFramesMatch() {
        val votes = FrameVotes(window = 5, needed = 3)
        assertEquals(emptyMap(), votes.add(good))
        assertEquals(emptyMap(), votes.add(misread))
        assertEquals(good - Field.SYSTOLIC, votes.add(good))
        assertEquals(good, votes.add(good))
    }

    @Test
    fun framesMissingDifferentValuesAddUp() {
        // A reflection hides one row or another, but never all three in the same frame.
        val votes = FrameVotes(window = 5, needed = 3)
        votes.add(good - Field.SYSTOLIC)
        votes.add(good - Field.DIASTOLIC)
        votes.add(good - Field.PULSE)
        assertEquals(good - Field.SYSTOLIC, votes.add(good - Field.SYSTOLIC))
        assertEquals(good, votes.add(good - Field.DIASTOLIC))
    }

    @Test
    fun oldFramesDropOut() {
        val votes = FrameVotes(window = 3, needed = 2)
        votes.add(good)
        votes.add(good)
        votes.add(emptyMap())
        assertEquals(good, votes.agreed())
        assertEquals(good - Field.SYSTOLIC, votes.add(misread))
    }

    @Test
    fun clearStartsAgain() {
        val votes = FrameVotes(window = 3, needed = 1)
        votes.add(good)
        votes.clear()
        assertTrue(votes.agreed().isEmpty())
    }
}

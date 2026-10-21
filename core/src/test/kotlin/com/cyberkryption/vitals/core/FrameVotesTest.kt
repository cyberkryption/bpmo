package com.cyberkryption.vitals.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FrameVotesTest {
    private val good = mapOf(Field.SYSTOLIC to 154, Field.DIASTOLIC to 100, Field.PULSE to 76)
    private val misread = good + (Field.SYSTOLIC to 184)

    @Test
    fun agreesOnceEnoughFramesMatch() {
        val votes = FrameVotes(window = 5, needed = 3)
        assertNull(votes.add(good))
        assertNull(votes.add(misread))
        assertNull(votes.add(good))
        assertEquals(good, votes.add(good))
    }

    @Test
    fun oldFramesDropOut() {
        val votes = FrameVotes(window = 3, needed = 2)
        votes.add(good)
        votes.add(good)
        votes.add(null)
        assertEquals(good, votes.agreed())
        votes.add(misread)
        assertNull(votes.agreed())
    }

    @Test
    fun clearStartsAgain() {
        val votes = FrameVotes(window = 3, needed = 1)
        votes.add(good)
        votes.clear()
        assertNull(votes.agreed())
    }
}

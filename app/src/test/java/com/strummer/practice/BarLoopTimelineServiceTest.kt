package com.strummer.practice

import com.strummer.practice.library.BarChordStep
import com.strummer.practice.library.BarLoopTimelineService
import org.junit.Assert.assertEquals
import org.junit.Test

class BarLoopTimelineServiceTest {
    private val service = BarLoopTimelineService()

    @Test
    fun resolvesChordAndLoopBarAcrossSequence() {
        val steps = listOf(
            BarChordStep("1", "song", 0, 1.0, 4.0, "G"),
            BarChordStep("2", "song", 1, 5.0, 4.0, "D"),
            BarChordStep("3", "song", 2, 9.0, 8.0, "Am")
        )

        val barMs = 2_400L // 4/4 at 100 bpm

        val atBar1 = service.resolve(0L, 100, 4, steps)
        val atBar5 = service.resolve(barMs * 4, 100, 4, steps)
        val atBar10 = service.resolve(barMs * 9, 100, 4, steps)
        val atBar17Loop = service.resolve(barMs * 16, 100, 4, steps)

        assertEquals("G", atBar1?.currentChord)
        assertEquals(1, atBar1?.currentStepNumber)
        assertEquals("D", atBar5?.currentChord)
        assertEquals(2, atBar5?.currentStepNumber)
        assertEquals("Am", atBar10?.currentChord)
        assertEquals(3, atBar10?.currentStepNumber)
        assertEquals(1.0, atBar17Loop?.loopBar ?: 0.0, 0.0001)
        assertEquals("G", atBar17Loop?.currentChord)
    }

    @Test
    fun resolvesHalfBarStepBoundaries() {
        val steps = listOf(
            BarChordStep("1", "song", 0, 1.0, 0.5, "G"),
            BarChordStep("2", "song", 1, 1.5, 0.5, "D")
        )

        val barMs = 2_400.0 // 4/4 at 100 bpm

        val atStart = service.resolve(0L, 100, 4, steps)
        val atHalfBar = service.resolve((barMs / 2.0).toLong(), 100, 4, steps)

        assertEquals("G", atStart?.currentChord)
        assertEquals("D", atHalfBar?.currentChord)
        assertEquals(1.5, atHalfBar?.loopBar ?: 0.0, 0.01)
    }
}

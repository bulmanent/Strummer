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
            BarChordStep("1", "song", 0, 1, 4, "G"),
            BarChordStep("2", "song", 1, 5, 4, "D"),
            BarChordStep("3", "song", 2, 9, 8, "Am")
        )

        val barMs = 2_400L // 4/4 at 100 bpm

        val atBar1 = service.resolve(0L, 100, 4, steps)
        val atBar5 = service.resolve(barMs * 4, 100, 4, steps)
        val atBar10 = service.resolve(barMs * 9, 100, 4, steps)
        val atBar17Loop = service.resolve(barMs * 16, 100, 4, steps)

        assertEquals("G", atBar1?.currentChord)
        assertEquals("D", atBar5?.currentChord)
        assertEquals("Am", atBar10?.currentChord)
        assertEquals(1, atBar17Loop?.loopBar)
        assertEquals("G", atBar17Loop?.currentChord)
    }
}

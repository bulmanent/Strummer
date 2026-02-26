package com.strummer.practice

import com.strummer.practice.library.ChordEvent
import com.strummer.practice.library.ChordTimelineService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChordTimelineServiceTest {
    private val service = ChordTimelineService()

    @Test
    fun sortsEventsByTimestamp() {
        val events = listOf(
            ChordEvent("b", "song", 3000, "D"),
            ChordEvent("a", "song", 1000, "G"),
            ChordEvent("c", "song", 1000, "Am")
        )

        val sorted = service.sortEvents(events)
        assertEquals(listOf("a", "c", "b"), sorted.map { it.id })
    }

    @Test
    fun returnsCurrentAndNextCueAtPlaybackTime() {
        val events = listOf(
            ChordEvent("e1", "song", 1000, "G"),
            ChordEvent("e2", "song", 2500, "C"),
            ChordEvent("e3", "song", 5000, "D")
        )

        val cue = service.cueAt(events, 2600)
        assertEquals("C", cue.current?.chordName)
        assertEquals("D", cue.next?.chordName)
    }

    @Test
    fun beforeFirstEventHasNoCurrentCue() {
        val events = listOf(ChordEvent("e1", "song", 1200, "Em"))

        val cue = service.cueAt(events, 100)
        assertNull(cue.current)
        assertEquals("Em", cue.next?.chordName)
    }
}

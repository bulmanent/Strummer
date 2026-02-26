package com.strummer.practice.detection

import com.strummer.practice.library.ChordEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordDraftMergeServiceTest {
    private val service = ChordDraftMergeService()

    @Test
    fun appliesSelectedSuggestionsOnlyAndPreservesManualEvents() {
        val existing = listOf(
            ChordEvent(id = "m1", songId = "song", timestampMs = 1000, chordName = "C", note = null),
            ChordEvent(id = "m2", songId = "song", timestampMs = 3000, chordName = "G", note = null)
        )
        val drafts = listOf(
            ChordEventDraft("d1", 2000, "Dm", 0.7f, "Dm", include = true),
            ChordEventDraft("d2", 3000, "Am", 0.8f, "Am", include = true),
            ChordEventDraft("d3", 4000, "F", 0.8f, "F", include = false)
        )

        val merged = service.merge(existing, drafts, songId = "song", replaceMode = false)

        assertEquals(listOf(1000L, 2000L, 3000L), merged.map { it.timestampMs })
        assertEquals("G", merged.first { it.timestampMs == 3000L }.chordName)
    }

    @Test
    fun replaceModeReplacesManualEventsDeterministically() {
        val existing = listOf(
            ChordEvent(id = "m1", songId = "song", timestampMs = 1000, chordName = "C")
        )
        val drafts = listOf(
            ChordEventDraft("d1", 500, "Dm", 0.9f, "Dm", include = true),
            ChordEventDraft("d2", 1500, "G", 0.9f, "G", include = true)
        )

        val merged = service.merge(existing, drafts, songId = "song", replaceMode = true)

        assertEquals(listOf(500L, 1500L), merged.map { it.timestampMs })
        assertTrue(merged.none { it.chordName == "C" })
    }
}

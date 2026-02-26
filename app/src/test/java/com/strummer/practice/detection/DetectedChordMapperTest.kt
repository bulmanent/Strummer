package com.strummer.practice.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectedChordMapperTest {
    private val mapper = DetectedChordMapper()

    @Test
    fun mapsDetectedEventsToEditableDrafts() {
        val drafts = mapper.toDrafts(
            listOf(
                DetectedChordEvent(timestampMs = 1000, chordName = " a min ", confidence = 0.8f),
                DetectedChordEvent(timestampMs = 2000, chordName = "G", confidence = 1.2f)
            )
        )

        assertEquals(2, drafts.size)
        assertEquals("Am", drafts.first().editableChordName)
        assertTrue(drafts[1].confidence <= 1f)
    }
}

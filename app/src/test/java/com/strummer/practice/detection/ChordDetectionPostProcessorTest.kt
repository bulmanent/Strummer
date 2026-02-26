package com.strummer.practice.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordDetectionPostProcessorTest {
    private val processor = ChordDetectionPostProcessor()

    @Test
    fun smoothsAndCollapsesUltraShortSegments() {
        val raw = listOf(
            DetectedChordEvent(timestampMs = 0, chordName = "C", confidence = 0.9f),
            DetectedChordEvent(timestampMs = 100, chordName = "G", confidence = 0.5f),
            DetectedChordEvent(timestampMs = 200, chordName = "C", confidence = 0.8f),
            DetectedChordEvent(timestampMs = 800, chordName = "Am", confidence = 0.7f)
        )

        val out = processor.process(raw, ChordDetectionParams(minSegmentDurationMs = 300, quantizeMs = 100))

        assertEquals(2, out.size)
        assertEquals("C", out[0].chordName)
        assertEquals("Am", out[1].chordName)
    }

    @Test
    fun enforcesOrderingAndConfidenceBounds() {
        val raw = listOf(
            DetectedChordEvent(timestampMs = 450, chordName = "G", confidence = 2.0f),
            DetectedChordEvent(timestampMs = 50, chordName = "C", confidence = -1.0f)
        )

        val out = processor.process(raw, ChordDetectionParams(quantizeMs = 100, minSegmentDurationMs = 0))

        assertEquals(listOf(100L, 500L), out.map { it.timestampMs })
        assertTrue(out.all { it.confidence in 0f..1f })
    }
}

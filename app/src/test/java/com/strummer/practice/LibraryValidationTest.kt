package com.strummer.practice

import com.strummer.practice.library.LibraryValidation
import com.strummer.practice.library.PlaybackPracticeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryValidationTest {
    @Test
    fun rejectsNegativeTimestamp() {
        assertEquals("Timestamp must be >= 0", LibraryValidation.validateChordEvent(-1L, "G"))
    }

    @Test
    fun rejectsInvalidPracticeConfigTargetLowerThanStart() {
        val config = PlaybackPracticeConfig(
            startSpeed = 1.0f,
            stepSize = 0.05f,
            targetSpeed = 0.8f,
            loopsPerSpeed = 1,
            loopEnabled = false,
            loopStartMs = null,
            loopEndMs = null
        )

        assertEquals(
            "Target speed cannot be lower than start speed",
            LibraryValidation.validatePracticeConfig(config, durationMs = 10_000L)
        )
    }

    @Test
    fun acceptsValidLoopRange() {
        val config = PlaybackPracticeConfig(
            startSpeed = 0.6f,
            stepSize = 0.05f,
            targetSpeed = 1.0f,
            loopsPerSpeed = 2,
            loopEnabled = true,
            loopStartMs = 500L,
            loopEndMs = 4000L
        )

        assertNull(LibraryValidation.validatePracticeConfig(config, durationMs = 20_000L))
    }
}

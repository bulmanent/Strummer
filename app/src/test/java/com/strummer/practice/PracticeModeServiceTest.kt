package com.strummer.practice

import com.strummer.practice.library.PlaybackPracticeConfig
import com.strummer.practice.library.PracticeModeService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeModeServiceTest {
    private val service = PracticeModeService()

    @Test
    fun incrementsSpeedAtConfiguredLoopBoundary() {
        val config = PlaybackPracticeConfig(
            startSpeed = 0.6f,
            stepSize = 0.1f,
            targetSpeed = 0.8f,
            loopsPerSpeed = 2,
            loopEnabled = true,
            loopStartMs = 1000L,
            loopEndMs = 5000L
        )

        val initial = service.initialState(config)
        val afterLoop1 = service.onLoopBoundary(config, initial)
        val afterLoop2 = service.onLoopBoundary(config, afterLoop1)

        assertEquals(0.6f, afterLoop1.currentSpeed, 0.0001f)
        assertEquals(0.7f, afterLoop2.currentSpeed, 0.0001f)
    }

    @Test
    fun doesNotIncreasePastTarget() {
        val config = PlaybackPracticeConfig(
            startSpeed = 0.95f,
            stepSize = 0.1f,
            targetSpeed = 1.0f,
            loopsPerSpeed = 1,
            loopEnabled = true,
            loopStartMs = 0L,
            loopEndMs = 2000L
        )

        val initial = service.initialState(config)
        val atTarget = service.onLoopBoundary(config, initial)

        assertEquals(1.0f, atTarget.currentSpeed, 0.0001f)
        assertTrue(atTarget.reachedTarget)
    }
}

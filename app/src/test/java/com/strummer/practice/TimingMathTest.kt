package com.strummer.practice

import com.strummer.practice.util.TimingMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimingMathTest {
    @Test
    fun framesPerStepFor8thAt120Bpm() {
        val frames = TimingMath.framesPerStep(sampleRate = 44_100, bpm = 120, subdivision = 8)
        assertEquals(11_025, frames)
    }

    @Test
    fun framesPerStepFor16thAt60Bpm() {
        val frames = TimingMath.framesPerStep(sampleRate = 44_100, bpm = 60, subdivision = 16)
        assertEquals(11_025, frames)
    }

    @Test
    fun beatBoundaryDetection() {
        val stepsPerBar = 8
        val beatsPerBar = 4
        assertTrue(TimingMath.isBeatBoundary(0, stepsPerBar, beatsPerBar))
        assertFalse(TimingMath.isBeatBoundary(1, stepsPerBar, beatsPerBar))
        assertTrue(TimingMath.isBeatBoundary(2, stepsPerBar, beatsPerBar))
        assertTrue(TimingMath.isDownbeat(0))
        assertFalse(TimingMath.isDownbeat(2))
    }
}

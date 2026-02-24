package com.strummer.practice.util

import kotlin.math.roundToInt

object TimingMath {
    fun framesPerStep(sampleRate: Int, bpm: Int, subdivision: Int): Int {
        require(sampleRate > 0)
        require(bpm in 1..300)
        require(subdivision == 8 || subdivision == 16)

        val quarterNoteSec = 60.0 / bpm
        val stepFraction = 4.0 / subdivision
        val secondsPerStep = quarterNoteSec * stepFraction
        return (secondsPerStep * sampleRate).roundToInt().coerceAtLeast(1)
    }

    fun isBeatBoundary(stepIndexInBar: Int, stepsPerBar: Int, beatsPerBar: Int): Boolean {
        if (stepsPerBar <= 0 || beatsPerBar <= 0) return false
        val stepsPerBeat = (stepsPerBar / beatsPerBar).coerceAtLeast(1)
        return stepIndexInBar % stepsPerBeat == 0
    }

    fun isDownbeat(stepIndexInBar: Int): Boolean = stepIndexInBar == 0
}

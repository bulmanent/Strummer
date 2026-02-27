package com.strummer.practice.library

data class BarLoopPosition(
    val absoluteBar: Double,
    val loopBar: Double,
    val currentStepNumber: Int?,
    val currentChord: String,
    val nextChord: String,
    val barsUntilNextChange: Double
)

class BarLoopTimelineService {
    fun resolve(
        elapsedMs: Long,
        tempoBpm: Int,
        timeSignatureTop: Int,
        steps: List<BarChordStep>
    ): BarLoopPosition? {
        if (steps.isEmpty() || tempoBpm <= 0 || timeSignatureTop <= 0) return null

        val barMs = (60_000.0 / tempoBpm.toDouble() * timeSignatureTop.toDouble()).coerceAtLeast(1.0)
        val ordered = steps.sortedBy { it.displayOrder }
        // Treat step starts as persistent loop markers; wrap at the last marker position.
        val totalBars = ordered.maxOf { it.startBar }.coerceAtLeast(1.0)

        val elapsedBars = elapsedMs.toDouble() / barMs
        val absoluteBar = elapsedBars + 1.0
        val loopBar = ((elapsedBars % totalBars) + totalBars) % totalBars + 1.0

        val currentIndex = ordered.indexOfLast { it.startBar <= loopBar + EPSILON }
            .let { if (it >= 0) it else ordered.lastIndex }
        val current = ordered[currentIndex]
        val nextIndex = if (ordered.size == 1) currentIndex else (currentIndex + 1) % ordered.size
        val next = ordered[nextIndex]
        val untilNext = if (ordered.size == 1) {
            0.0
        } else if (next.startBar > loopBar + EPSILON) {
            next.startBar - loopBar
        } else {
            totalBars - loopBar + next.startBar
        }

        return BarLoopPosition(
            absoluteBar = absoluteBar,
            loopBar = loopBar,
            currentStepNumber = currentIndex + 1,
            currentChord = current.chordName,
            nextChord = next.chordName,
            barsUntilNextChange = untilNext.coerceAtLeast(0.0)
        )
    }

    private companion object {
        const val EPSILON = 1e-6
    }
}

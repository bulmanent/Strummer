package com.strummer.practice.library

data class BarLoopPosition(
    val absoluteBar: Int,
    val loopBar: Int,
    val currentStepNumber: Int?,
    val currentChord: String,
    val nextChord: String,
    val barsUntilNextChange: Int
)

class BarLoopTimelineService {
    fun resolve(
        elapsedMs: Long,
        tempoBpm: Int,
        timeSignatureTop: Int,
        steps: List<BarChordStep>
    ): BarLoopPosition? {
        if (steps.isEmpty() || tempoBpm <= 0 || timeSignatureTop <= 0) return null

        val barMs = (60_000.0 / tempoBpm.toDouble() * timeSignatureTop.toDouble()).toLong().coerceAtLeast(1L)
        val ordered = steps.sortedBy { it.displayOrder }
        val totalBars = ordered.maxOf { it.startBar + it.barCount - 1 }.coerceAtLeast(1)

        val elapsedBars = (elapsedMs / barMs).toInt()
        val absoluteBar = elapsedBars + 1
        val loopBar = (elapsedBars % totalBars) + 1

        val currentIndex = ordered.indexOfFirst { loopBar in it.startBar..(it.startBar + it.barCount - 1) }
        val current = ordered.getOrNull(currentIndex)
        val next = when {
            currentIndex >= 0 -> ordered.getOrNull(currentIndex + 1) ?: ordered.first()
            else -> ordered.firstOrNull { it.startBar > loopBar } ?: ordered.first()
        }
        val untilNext = if (next.startBar > loopBar) next.startBar - loopBar else totalBars - loopBar + next.startBar

        return BarLoopPosition(
            absoluteBar = absoluteBar,
            loopBar = loopBar,
            currentStepNumber = if (currentIndex >= 0) currentIndex + 1 else null,
            currentChord = current?.chordName ?: "-",
            nextChord = next.chordName,
            barsUntilNextChange = untilNext.coerceAtLeast(1)
        )
    }
}

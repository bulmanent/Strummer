package com.strummer.practice.library

data class BarLoopPosition(
    val absoluteBar: Int,
    val loopBar: Int,
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

        val current = ordered.firstOrNull { loopBar in it.startBar..(it.startBar + it.barCount - 1) }
            ?: ordered.lastOrNull { it.startBar <= loopBar }
            ?: ordered.last()

        val next = ordered.firstOrNull { it.startBar > current.startBar } ?: ordered.first()
        val untilNext = if (next.startBar > loopBar) {
            next.startBar - loopBar
        } else {
            totalBars - loopBar + next.startBar
        }

        return BarLoopPosition(
            absoluteBar = absoluteBar,
            loopBar = loopBar,
            currentChord = current.chordName,
            nextChord = next.chordName,
            barsUntilNextChange = untilNext.coerceAtLeast(1)
        )
    }
}

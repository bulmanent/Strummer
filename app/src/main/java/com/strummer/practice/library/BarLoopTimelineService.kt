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
        val firstStartBar = ordered.first().startBar
        val repeatBars = ordered.sumOf { it.barCount }.coerceAtLeast(EPSILON)
        val firstPassEndExclusive = ordered.last().startBar + ordered.last().barCount

        val elapsedBars = elapsedMs.toDouble() / barMs
        val absoluteBar = elapsedBars + 1.0
        val (loopBar, currentIndex, untilNext) = when {
            absoluteBar < firstStartBar - EPSILON -> {
                val untilFirst = (firstStartBar - absoluteBar).coerceAtLeast(0.0)
                Triple(1.0, 0, untilFirst)
            }
            absoluteBar < firstPassEndExclusive - EPSILON -> {
                val current = ordered.indices.lastOrNull { idx ->
                    val start = ordered[idx].startBar
                    val end = ordered.getOrNull(idx + 1)?.startBar ?: firstPassEndExclusive
                    absoluteBar + EPSILON >= start && absoluteBar < end - EPSILON
                } ?: ordered.indices.lastOrNull { absoluteBar + EPSILON >= ordered[it].startBar } ?: 0
                val end = ordered.getOrNull(current + 1)?.startBar ?: firstPassEndExclusive
                val until = (end - absoluteBar).coerceAtLeast(0.0)
                val localLoopBar = (absoluteBar - firstStartBar + 1.0).coerceAtLeast(1.0)
                Triple(localLoopBar, current, until)
            }
            else -> {
                val barsIntoRepeating = absoluteBar - firstPassEndExclusive
                val repeatingPos = ((barsIntoRepeating % repeatBars) + repeatBars) % repeatBars
                var cumulative = 0.0
                var idx = 0
                while (idx < ordered.size - 1 && repeatingPos >= cumulative + ordered[idx].barCount - EPSILON) {
                    cumulative += ordered[idx].barCount
                    idx += 1
                }
                val until = (cumulative + ordered[idx].barCount - repeatingPos).coerceAtLeast(0.0)
                val localLoopBar = repeatingPos + 1.0
                Triple(localLoopBar, idx, until)
            }
        }
        val current = ordered[currentIndex]
        val next = if (ordered.size == 1) current else ordered[(currentIndex + 1) % ordered.size]

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

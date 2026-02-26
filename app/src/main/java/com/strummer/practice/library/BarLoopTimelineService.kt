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
        val totalBars = steps.sumOf { it.barCount }.coerceAtLeast(1)

        val elapsedBars = (elapsedMs / barMs).toInt()
        val absoluteBar = elapsedBars + 1
        val loopBar = (elapsedBars % totalBars) + 1

        var running = 0
        var current = steps.first()
        var next = steps.first()
        var untilNext = 1

        for (index in steps.indices) {
            val step = steps[index]
            val start = running + 1
            val end = running + step.barCount
            if (loopBar in start..end) {
                current = step
                next = steps[(index + 1) % steps.size]
                untilNext = end - loopBar + 1
                break
            }
            running = end
        }

        return BarLoopPosition(
            absoluteBar = absoluteBar,
            loopBar = loopBar,
            currentChord = current.chordName,
            nextChord = next.chordName,
            barsUntilNextChange = untilNext
        )
    }
}

package com.strummer.practice.detection

class ChordDetectionPostProcessor {
    fun process(raw: List<DetectedChordEvent>, params: ChordDetectionParams): List<DetectedChordEvent> {
        if (raw.isEmpty()) return emptyList()

        val quantized = raw
            .map {
                it.copy(
                    timestampMs = quantize(it.timestampMs, params.quantizeMs),
                    confidence = it.confidence.coerceIn(0f, 1f)
                )
            }
            .sortedBy { it.timestampMs }
            .distinctBy { it.timestampMs to it.chordName }

        if (quantized.isEmpty()) return emptyList()

        val smoothed = smoothIsolatedFlips(quantized)
        val collapsed = collapseConsecutive(smoothed)
        val reduced = collapseShortSegments(collapsed, params.minSegmentDurationMs)

        return reduced
            .sortedBy { it.timestampMs }
            .take(params.maxSuggestions)
    }

    private fun smoothIsolatedFlips(events: List<DetectedChordEvent>): List<DetectedChordEvent> {
        if (events.size < 3) return events
        val mutable = events.toMutableList()

        for (idx in 1 until events.lastIndex) {
            val prev = mutable[idx - 1]
            val current = mutable[idx]
            val next = mutable[idx + 1]
            if (prev.chordName == next.chordName && current.chordName != prev.chordName) {
                val confidence = ((prev.confidence + next.confidence) / 2f).coerceIn(0f, 1f)
                mutable[idx] = current.copy(chordName = prev.chordName, confidence = confidence)
            }
        }

        return mutable
    }

    private fun collapseConsecutive(events: List<DetectedChordEvent>): List<DetectedChordEvent> {
        if (events.isEmpty()) return events

        val out = mutableListOf<DetectedChordEvent>()
        for (event in events) {
            val last = out.lastOrNull()
            if (last == null || last.chordName != event.chordName) {
                out += event
                continue
            }

            if (event.confidence > last.confidence) {
                out[out.lastIndex] = last.copy(confidence = event.confidence)
            }
        }
        return out
    }

    private fun collapseShortSegments(events: List<DetectedChordEvent>, minDurationMs: Long): List<DetectedChordEvent> {
        if (events.size < 3 || minDurationMs <= 0L) return events
        val result = events.toMutableList()

        var changed = true
        while (changed) {
            changed = false
            var i = 1
            while (i < result.lastIndex) {
                val prev = result[i - 1]
                val current = result[i]
                val next = result[i + 1]
                val duration = next.timestampMs - current.timestampMs
                if (duration in 0 until minDurationMs && prev.chordName == next.chordName) {
                    result.removeAt(i)
                    changed = true
                    continue
                }
                i += 1
            }
        }

        return collapseConsecutive(result)
    }

    private fun quantize(timestampMs: Long, gridMs: Long): Long {
        if (gridMs <= 1L) return timestampMs.coerceAtLeast(0L)
        val safe = timestampMs.coerceAtLeast(0L)
        val remainder = safe % gridMs
        return if (remainder >= gridMs / 2) safe + (gridMs - remainder) else safe - remainder
    }
}

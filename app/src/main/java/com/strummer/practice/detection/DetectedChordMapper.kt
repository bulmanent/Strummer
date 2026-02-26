package com.strummer.practice.detection

import java.util.UUID

class DetectedChordMapper {
    fun toDrafts(events: List<DetectedChordEvent>): List<ChordEventDraft> {
        return events.sortedBy { it.timestampMs }
            .map { event ->
                ChordEventDraft(
                    draftId = UUID.randomUUID().toString(),
                    timestampMs = event.timestampMs,
                    suggestedChord = normalizeChordName(event.chordName),
                    confidence = event.confidence.coerceIn(0f, 1f),
                    editableChordName = normalizeChordName(event.chordName),
                    include = true,
                    note = event.rawLabel
                )
            }
    }

    fun normalizeChordName(raw: String): String {
        val clean = raw.trim().replace("min", "m", ignoreCase = true)
        val normalized = clean.replace(Regex("\\s+"), "")
        if (normalized.isBlank()) return "C"
        val candidate = normalized.replaceFirstChar { it.uppercase() }
        return when (candidate) {
            "A", "Am", "B", "Bm", "C", "Cm", "D", "Dm", "E", "Em", "F", "Fm", "G", "Gm",
            "A#", "A#m", "C#", "C#m", "D#", "D#m", "F#", "F#m", "G#", "G#m" -> candidate
            else -> {
                val base = candidate.replace(Regex("[^A-G#m#]"), "").ifBlank { "C" }
                if (base.length >= 2 && base[1] == '#') {
                    val tail = base.drop(2).replace("M", "m")
                    "${base[0]}#${tail}"
                } else {
                    "${base[0]}${base.drop(1).replace("M", "m")}"
                }
            }
        }
    }
}

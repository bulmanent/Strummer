package com.strummer.practice.detection

import com.strummer.practice.library.ChordEvent
import java.util.UUID

class ChordDraftMergeService {
    fun merge(
        existing: List<ChordEvent>,
        drafts: List<ChordEventDraft>,
        songId: String,
        replaceMode: Boolean
    ): List<ChordEvent> {
        val accepted = drafts
            .asSequence()
            .filter { it.include }
            .mapNotNull { draft ->
                val chord = draft.editableChordName.trim()
                if (draft.timestampMs < 0L || chord.isBlank()) return@mapNotNull null
                ChordEvent(
                    id = UUID.randomUUID().toString(),
                    songId = songId,
                    timestampMs = draft.timestampMs,
                    chordName = chord,
                    note = draft.note
                )
            }
            .toList()

        if (replaceMode) {
            return accepted
                .sortedWith(compareBy<ChordEvent> { it.timestampMs }.thenBy { it.id })
                .dedupeByTimestamp()
        }

        val existingByTimestamp = existing.associateBy { it.timestampMs }
        val merged = buildList {
            addAll(existing)
            accepted.forEach { detected ->
                if (!existingByTimestamp.containsKey(detected.timestampMs)) {
                    add(detected)
                }
            }
        }

        return merged
            .sortedWith(compareBy<ChordEvent> { it.timestampMs }.thenBy { it.id })
            .dedupeByTimestamp()
    }

    private fun List<ChordEvent>.dedupeByTimestamp(): List<ChordEvent> {
        return this
            .groupBy { it.timestampMs }
            .map { (_, eventsAtTime) ->
                eventsAtTime.firstOrNull { it.note == null } ?: eventsAtTime.first()
            }
            .sortedWith(compareBy<ChordEvent> { it.timestampMs }.thenBy { it.id })
    }
}

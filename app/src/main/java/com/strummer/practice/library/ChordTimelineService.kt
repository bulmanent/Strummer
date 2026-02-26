package com.strummer.practice.library

class ChordTimelineService {
    fun sortEvents(events: List<ChordEvent>): List<ChordEvent> {
        return events.sortedWith(compareBy<ChordEvent> { it.timestampMs }.thenBy { it.id })
    }

    fun cueAt(events: List<ChordEvent>, positionMs: Long): ActiveChordCue {
        if (events.isEmpty()) return ActiveChordCue(current = null, next = null)

        val sorted = sortEvents(events)
        var current: ChordEvent? = null
        var next: ChordEvent? = null

        for (event in sorted) {
            if (event.timestampMs <= positionMs) {
                current = event
            } else {
                next = event
                break
            }
        }

        if (current == null) {
            next = sorted.firstOrNull()
        }

        return ActiveChordCue(current = current, next = next)
    }
}

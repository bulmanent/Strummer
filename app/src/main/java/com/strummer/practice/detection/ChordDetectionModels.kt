package com.strummer.practice.detection

data class ChordDetectionParams(
    val windowMs: Long = 500L,
    val minSegmentDurationMs: Long = 300L,
    val quantizeMs: Long = 100L,
    val maxSuggestions: Int = 600
)

data class DetectedChordEvent(
    val timestampMs: Long,
    val chordName: String,
    val confidence: Float,
    val source: String = "auto",
    val rawLabel: String? = null
)

data class ChordEventDraft(
    val draftId: String,
    val timestampMs: Long,
    val suggestedChord: String,
    val confidence: Float,
    val editableChordName: String,
    val include: Boolean = true,
    val note: String? = null
)

sealed class ChordDetectionOutcome {
    data class Success(
        val events: List<DetectedChordEvent>,
        val averageConfidence: Float,
        val detectorVersion: String
    ) : ChordDetectionOutcome()

    data class Failure(val message: String) : ChordDetectionOutcome()
}

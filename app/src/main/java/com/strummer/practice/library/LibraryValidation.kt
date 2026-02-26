package com.strummer.practice.library

object LibraryValidation {
    private const val MIN_SPEED = 0.5f
    private const val MAX_SPEED = 1.25f

    fun validateSongInput(title: String, audioFilePath: String): String? {
        if (title.isBlank()) return "Song title is required"
        if (audioFilePath.isBlank()) return "Audio file path is required"
        return null
    }

    fun validateChordEvent(timestampMs: Long, chordName: String): String? {
        if (timestampMs < 0L) return "Timestamp must be >= 0"
        if (chordName.isBlank()) return "Chord name is required"
        return null
    }

    fun validatePracticeConfig(config: PlaybackPracticeConfig, durationMs: Long?): String? {
        if (config.startSpeed !in MIN_SPEED..MAX_SPEED) return "Start speed must be between 0.5x and 1.25x"
        if (config.targetSpeed !in MIN_SPEED..MAX_SPEED) return "Target speed must be between 0.5x and 1.25x"
        if (config.targetSpeed + 1e-6f < config.startSpeed) return "Target speed cannot be lower than start speed"
        if (config.stepSize <= 0f) return "Step size must be greater than 0"
        if (config.loopsPerSpeed <= 0) return "Loops per speed must be at least 1"

        if (config.loopEnabled) {
            val start = config.loopStartMs ?: return "Loop start is required when looping is enabled"
            val end = config.loopEndMs ?: return "Loop end is required when looping is enabled"
            if (start < 0L || end < 0L) return "Loop range must be >= 0"
            if (end <= start) return "Loop end must be greater than loop start"
            if (end - start < 250L) return "Loop range is too short (minimum 250ms)"
            if (durationMs != null && end > durationMs) return "Loop end cannot exceed song duration"
        }

        return null
    }
}

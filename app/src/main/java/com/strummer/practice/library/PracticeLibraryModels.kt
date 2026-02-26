package com.strummer.practice.library

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String? = null,
    val audioFilePath: String,
    val durationMs: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ChordEvent(
    val id: String,
    val songId: String,
    val timestampMs: Long,
    val chordName: String,
    val note: String? = null
)

@Serializable
data class PracticeProfile(
    val songId: String,
    val startSpeed: Float = 0.6f,
    val stepSize: Float = 0.05f,
    val targetSpeed: Float = 1.0f,
    val loopsPerSpeed: Int = 1,
    val loopEnabled: Boolean = false,
    val loopStartMs: Long? = null,
    val loopEndMs: Long? = null
)

@Serializable
data class PracticeLibraryState(
    val schemaVersion: Int = 1,
    val songs: List<Song> = emptyList(),
    val chordEvents: List<ChordEvent> = emptyList(),
    val practiceProfiles: List<PracticeProfile> = emptyList()
)

data class PlaybackPracticeConfig(
    val startSpeed: Float,
    val stepSize: Float,
    val targetSpeed: Float,
    val loopsPerSpeed: Int,
    val loopEnabled: Boolean,
    val loopStartMs: Long?,
    val loopEndMs: Long?
)

data class ActiveChordCue(
    val current: ChordEvent?,
    val next: ChordEvent?
)

package com.strummer.practice.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongsAsset(
    val songs: List<Song>
)

@Serializable
data class Song(
    val title: String,
    val sections: List<Section>
)

@Serializable
data class Section(
    val name: String,
    val bars: List<Bar>
)

@Serializable
data class Bar(
    val chord: String,
    val beatsPerBar: Int = 4,
    val patternId: String
)

@Serializable
data class PatternsAsset(
    val patterns: List<StrumPattern>
)

@Serializable
data class StrumPattern(
    val id: String,
    val name: String,
    val subdivision: Int,
    val steps: List<PatternStep>
)

@Serializable
data class PatternStep(
    @SerialName("kind")
    val kindRaw: String,
    val accent: Boolean = false
) {
    val kind: StepKind
        get() = StepKind.fromToken(kindRaw)
}

enum class StepKind(val token: String) {
    Down("D"),
    Up("U"),
    Mute("X"),
    Rest("-");

    companion object {
        fun fromToken(token: String): StepKind {
            return when (token.trim().uppercase()) {
                "D" -> Down
                "U" -> Up
                "X" -> Mute
                "-" -> Rest
                else -> throw IllegalArgumentException("Unsupported step token: $token")
            }
        }
    }
}

data class PlaybackBar(
    val chord: String,
    val beatsPerBar: Int,
    val pattern: StrumPattern
)

data class RampConfig(
    val enabled: Boolean,
    val startBpm: Int,
    val endBpm: Int,
    val increment: Int,
    val barsPerIncrement: Int
)

data class RampProgress(
    val active: Boolean = false,
    val currentBpm: Int = 0,
    val barsUntilIncrement: Int = 0
)

data class ChordShape(
    val name: String,
    val frets: List<Int>
)

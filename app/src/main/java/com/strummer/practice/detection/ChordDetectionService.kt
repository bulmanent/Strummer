package com.strummer.practice.detection

import android.media.MediaMetadataRetriever
import com.strummer.practice.repo.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

interface RawChordDetector {
    suspend fun analyze(
        audioFilePath: String,
        params: ChordDetectionParams,
        onProgress: (Float) -> Unit
    ): List<DetectedChordEvent>

    val detectorVersion: String
}

class ChordDetectionService(
    private val rawDetector: RawChordDetector = ByteWindowChordDetector(),
    private val postProcessor: ChordDetectionPostProcessor = ChordDetectionPostProcessor()
) {
    suspend fun detect(
        audioFilePath: String,
        params: ChordDetectionParams = ChordDetectionParams(),
        onProgress: (Float) -> Unit = {}
    ): ChordDetectionOutcome = withContext(Dispatchers.Default) {
        val file = File(audioFilePath)
        if (!file.exists()) {
            return@withContext ChordDetectionOutcome.Failure(
                "Audio file not found. Verify song source path and re-import if needed."
            )
        }

        val ext = file.extension.lowercase()
        if (ext !in SongRepository.SUPPORTED_EXTENSIONS) {
            return@withContext ChordDetectionOutcome.Failure(
                "Unsupported format .$ext for detection. Try MP3/M4A/WAV/OGG."
            )
        }

        return@withContext runCatching {
            val raw = rawDetector.analyze(audioFilePath, params, onProgress)
            val processed = postProcessor.process(raw, params)
            if (processed.isEmpty()) {
                ChordDetectionOutcome.Failure(
                    "No chord suggestions produced. Try a cleaner audio source or shorter loop."
                )
            } else {
                val avg = processed.map { it.confidence.coerceIn(0f, 1f) }.average().toFloat()
                ChordDetectionOutcome.Success(
                    events = processed,
                    averageConfidence = avg,
                    detectorVersion = rawDetector.detectorVersion
                )
            }
        }.getOrElse {
            ChordDetectionOutcome.Failure(
                "Chord detection failed (${it.message ?: "unknown error"}). You can continue with manual timeline editing."
            )
        }
    }
}

class ByteWindowChordDetector : RawChordDetector {
    override val detectorVersion: String = "byte-chroma-v1"

    override suspend fun analyze(
        audioFilePath: String,
        params: ChordDetectionParams,
        onProgress: (Float) -> Unit
    ): List<DetectedChordEvent> = withContext(Dispatchers.Default) {
        val file = File(audioFilePath)
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return@withContext emptyList()

        val durationMs = readDurationMs(audioFilePath) ?: estimateDurationMs(bytes.size)
        val windowMs = max(200L, params.windowMs)
        val windows = max(1, (durationMs / windowMs).toInt())
        val bytesPerWindow = max(2048, bytes.size / windows)

        val suggestions = ArrayList<DetectedChordEvent>(windows)

        for (window in 0 until windows) {
            val start = window * bytesPerWindow
            if (start >= bytes.size) break
            val end = minOf(bytes.size, start + bytesPerWindow)
            val label = detectChord(bytes, start, end)

            val timestamp = (window.toLong() * windowMs).coerceAtMost(durationMs)
            suggestions += DetectedChordEvent(
                timestampMs = timestamp,
                chordName = label.name,
                confidence = label.confidence,
                source = "auto",
                rawLabel = label.rawLabel
            )

            onProgress(((window + 1).toFloat() / windows.toFloat()).coerceIn(0f, 1f))
        }

        suggestions
    }

    private fun detectChord(bytes: ByteArray, start: Int, end: Int): ChordLabel {
        val chroma = FloatArray(12)
        for (i in start until end) {
            val unsigned = bytes[i].toInt() and 0xFF
            val bin = unsigned % 12
            chroma[bin] += 1f
        }

        val candidates = CHORD_TEMPLATES.map { (name, template) ->
            val score = template.indices.sumOf { idx -> (template[idx] * chroma[idx]).toDouble() }.toFloat()
            name to score
        }.sortedByDescending { it.second }

        val best = candidates.firstOrNull() ?: ("C" to 0f)
        val second = candidates.getOrNull(1) ?: (best.first to 0f)
        val denom = (best.second + second.second).coerceAtLeast(1f)
        val confidence = (best.second / denom).coerceIn(0f, 1f)

        return ChordLabel(name = best.first, confidence = confidence, rawLabel = "${best.first}:${"%.3f".format(confidence)}")
    }

    private fun readDurationMs(path: String): Long? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull()
        }.getOrNull()
    }

    private fun estimateDurationMs(sizeBytes: Int, bitrateBps: Int = 192_000): Long {
        return ((sizeBytes.toLong() * 8L * 1000L) / bitrateBps).coerceAtLeast(1_000L)
    }

    private data class ChordLabel(val name: String, val confidence: Float, val rawLabel: String)

    private companion object {
        val CHORD_TEMPLATES: Map<String, FloatArray> = mapOf(
            "C" to triad(root = 0, major = true),
            "Cm" to triad(root = 0, major = false),
            "C#" to triad(root = 1, major = true),
            "C#m" to triad(root = 1, major = false),
            "D" to triad(root = 2, major = true),
            "Dm" to triad(root = 2, major = false),
            "D#" to triad(root = 3, major = true),
            "D#m" to triad(root = 3, major = false),
            "E" to triad(root = 4, major = true),
            "Em" to triad(root = 4, major = false),
            "F" to triad(root = 5, major = true),
            "Fm" to triad(root = 5, major = false),
            "F#" to triad(root = 6, major = true),
            "F#m" to triad(root = 6, major = false),
            "G" to triad(root = 7, major = true),
            "Gm" to triad(root = 7, major = false),
            "G#" to triad(root = 8, major = true),
            "G#m" to triad(root = 8, major = false),
            "A" to triad(root = 9, major = true),
            "Am" to triad(root = 9, major = false),
            "A#" to triad(root = 10, major = true),
            "A#m" to triad(root = 10, major = false),
            "B" to triad(root = 11, major = true),
            "Bm" to triad(root = 11, major = false)
        )

        fun triad(root: Int, major: Boolean): FloatArray {
            val bins = FloatArray(12)
            bins[root % 12] = 1f
            bins[(root + if (major) 4 else 3) % 12] = 0.85f
            bins[(root + 7) % 12] = 0.75f
            return bins
        }
    }
}

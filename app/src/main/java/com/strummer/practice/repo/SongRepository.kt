package com.strummer.practice.repo

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.strummer.practice.library.AudioFileChecker
import com.strummer.practice.library.AudioFileStatus
import com.strummer.practice.library.BarChordStep
import com.strummer.practice.library.LibraryValidation
import com.strummer.practice.library.PracticeLibraryState
import com.strummer.practice.library.PracticeProfile
import com.strummer.practice.library.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

class SongRepository(private val context: Context) {
    private val mutex = Mutex()
    private val store = PracticeLibraryStore(
        storageFile = File(context.filesDir, "practice-library/library_state.json"),
        backupFile = File(context.filesDir, "practice-library/library_state.json.bak")
    )

    private val stateFlow = MutableStateFlow(store.read())
    private val audioFileChecker = AudioFileChecker()

    fun songsFlow(): Flow<List<Song>> = stateFlow.map { it.songs.sortedByDescending(Song::updatedAt) }

    fun barStepsFlow(songId: String): Flow<List<BarChordStep>> =
        stateFlow.map { state ->
            normalizeStepStarts(
                state.barChordSteps
                .filter { it.songId == songId }
                .sortedBy { it.displayOrder }
            )
        }

    fun practiceProfileFlow(songId: String): Flow<PracticeProfile> =
        stateFlow.map { state ->
            state.practiceProfiles.firstOrNull { it.songId == songId }
                ?: PracticeProfile(songId = songId)
        }

    suspend fun addSong(title: String, audioUri: Uri): Song {
        val sourceName = guessSourceName(audioUri)
        val ext = sourceName.substringAfterLast('.', "").lowercase()
        if (ext !in SUPPORTED_EXTENSIONS) {
            throw IllegalArgumentException("Unsupported file type .$ext. Supported: ${SUPPORTED_EXTENSIONS.joinToString()}")
        }

        val audioFolder = File(context.filesDir, "practice-library/audio")
        audioFolder.mkdirs()
        val destFile = File(audioFolder, "${UUID.randomUUID()}.$ext")

        copyAudio(context.contentResolver, audioUri, destFile)

        val duration = extractDuration(destFile.absolutePath)
        val now = System.currentTimeMillis()
        val error = LibraryValidation.validateSongInput(title, destFile.absolutePath)
        require(error == null) { error ?: "Invalid song input" }

        val song = Song(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            artist = null,
            audioFilePath = destFile.absolutePath,
            durationMs = duration,
            createdAt = now,
            updatedAt = now
        )

        mutateState { state ->
            state.copy(songs = state.songs + song)
        }

        Log.i(TAG, "Imported song ${song.title} from uri=$audioUri path=${song.audioFilePath}")
        return song
    }

    suspend fun updateSongTitle(songId: String, title: String): Song {
        val state = stateFlow.value
        val existing = state.songs.firstOrNull { it.id == songId }
            ?: throw IllegalArgumentException("Song not found")
        val error = LibraryValidation.validateSongInput(title, existing.audioFilePath)
        require(error == null) { error ?: "Invalid song input" }

        val updated = existing.copy(
            title = title.trim(),
            updatedAt = System.currentTimeMillis()
        )

        mutateState { current ->
            current.copy(
                songs = current.songs.map { if (it.id == songId) updated else it }
            )
        }

        return updated
    }

    suspend fun deleteSong(songId: String) {
        val song = stateFlow.value.songs.firstOrNull { it.id == songId } ?: return
        mutateState { state ->
            state.copy(
                songs = state.songs.filterNot { it.id == songId },
                chordEvents = state.chordEvents.filterNot { it.songId == songId },
                practiceProfiles = state.practiceProfiles.filterNot { it.songId == songId },
                barChordSteps = state.barChordSteps.filterNot { it.songId == songId }
            )
        }

        runCatching {
            val file = File(song.audioFilePath)
            if (file.exists()) file.delete()
        }.onFailure { Log.w(TAG, "Failed to delete audio file for ${song.id}", it) }
    }

    suspend fun addBarStep(songId: String, chordName: String, barCount: Int): BarChordStep {
        require(barCount > 0) { "Bars must be at least 1" }
        require(chordName.isNotBlank()) { "Chord is required" }
        ensureSongExists(songId)
        val normalizedChord = normalizeChord(chordName)

        val existing = normalizeStepStarts(
            stateFlow.value.barChordSteps
            .filter { it.songId == songId }
            .sortedBy { it.displayOrder }
        )
        val nextOrder = existing.maxOfOrNull { it.displayOrder + 1 }
            ?: 0
        val nextStart = existing.maxOfOrNull { it.startBar + it.barCount } ?: 1

        val step = BarChordStep(
            id = UUID.randomUUID().toString(),
            songId = songId,
            displayOrder = nextOrder,
            startBar = nextStart,
            barCount = barCount,
            chordName = normalizedChord
        )

        mutateState { state -> state.copy(barChordSteps = state.barChordSteps + step) }
        return step
    }

    suspend fun updateBarStep(step: BarChordStep): BarChordStep {
        require(step.startBar >= 1) { "Start bar must be at least 1" }
        require(step.barCount > 0) { "Bars must be at least 1" }
        require(step.chordName.isNotBlank()) { "Chord is required" }
        val normalized = step.copy(chordName = normalizeChord(step.chordName))

        mutateState { state ->
            val exists = state.barChordSteps.any { it.id == normalized.id }
            require(exists) { "Step not found" }
            state.copy(barChordSteps = state.barChordSteps.map { if (it.id == normalized.id) normalized else it })
        }

        return normalized
    }

    suspend fun updateBarStepStartBar(stepId: String, startBar: Int) {
        require(startBar >= 1) { "Start bar must be at least 1" }
        mutateState { state ->
            if (state.barChordSteps.none { it.id == stepId }) {
                throw IllegalArgumentException("Step not found")
            }
            state.copy(
                barChordSteps = state.barChordSteps.map {
                    if (it.id == stepId) it.copy(startBar = startBar) else it
                }
            )
        }
    }

    suspend fun deleteBarStep(stepId: String) {
        mutateState { state ->
            val remaining = state.barChordSteps.filterNot { it.id == stepId }
            val reindexed = remaining
                .groupBy { it.songId }
                .values
                .flatMap { list ->
                    list.sortedBy { it.displayOrder }.mapIndexed { index, step ->
                        step.copy(displayOrder = index)
                    }
                }
            state.copy(barChordSteps = reindexed)
        }
    }

    suspend fun upsertPracticeProfile(songId: String, tempoBpm: Int, timeSignatureTop: Int, timeSignatureBottom: Int) {
        ensureSongExists(songId)
        require(tempoBpm in 40..220) { "Tempo must be between 40 and 220 BPM" }
        require(timeSignatureTop in 2..12) { "Invalid time signature" }
        require(timeSignatureBottom in setOf(2, 4, 8)) { "Invalid time signature denominator" }

        val saved = PracticeProfile(
            songId = songId,
            tempoBpm = tempoBpm,
            timeSignatureTop = timeSignatureTop,
            timeSignatureBottom = timeSignatureBottom
        )

        mutateState { state ->
            val profiles = state.practiceProfiles.filterNot { it.songId == songId } + saved
            state.copy(practiceProfiles = profiles)
        }
    }

    fun audioFileStatus(song: Song): AudioFileStatus {
        return audioFileChecker.status(song.audioFilePath)
    }

    private suspend fun mutateState(transform: (PracticeLibraryState) -> PracticeLibraryState) {
        mutex.withLock {
            val next = transform(stateFlow.value)
            store.write(next)
            stateFlow.update { next }
        }
    }

    private fun ensureSongExists(songId: String) {
        require(stateFlow.value.songs.any { it.id == songId }) { "Song not found" }
    }

    private fun guessSourceName(audioUri: Uri): String {
        val resolver = context.contentResolver
        resolver.query(audioUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index) ?: "import.mp3"
            }
        }
        return audioUri.lastPathSegment ?: "import.mp3"
    }

    private fun copyAudio(contentResolver: ContentResolver, audioUri: Uri, destination: File) {
        val input = contentResolver.openInputStream(audioUri)
            ?: throw IllegalArgumentException("Cannot open selected audio file")
        input.use { source ->
            destination.outputStream().use { sink ->
                source.copyTo(sink)
            }
        }
    }

    private fun extractDuration(path: String): Long? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            raw?.toLongOrNull()
        }.onFailure {
            Log.w(TAG, "Unable to read audio duration for $path", it)
        }.getOrNull()
    }

    private fun normalizeChord(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return trimmed
        val first = trimmed.first()
        return if (first.isLetter()) {
            first.uppercaseChar().toString() + trimmed.drop(1)
        } else {
            trimmed
        }
    }

    private fun normalizeStepStarts(steps: List<BarChordStep>): List<BarChordStep> {
        if (steps.isEmpty()) return steps
        val ordered = steps.sortedBy { it.displayOrder }
        val alreadyValid = ordered.zipWithNext().all { (a, b) -> b.startBar > a.startBar } &&
            ordered.first().startBar >= 1
        if (alreadyValid) return ordered

        var start = 1
        return ordered.map { step ->
            val normalized = step.copy(startBar = start)
            start += step.barCount
            normalized
        }
    }

    companion object {
        private const val TAG = "SongRepository"
        val SUPPORTED_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg")
    }
}

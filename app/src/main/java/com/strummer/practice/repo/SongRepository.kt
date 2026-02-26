package com.strummer.practice.repo

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.strummer.practice.library.AudioFileChecker
import com.strummer.practice.library.AudioFileStatus
import com.strummer.practice.library.ChordEvent
import com.strummer.practice.library.LibraryValidation
import com.strummer.practice.library.PlaybackPracticeConfig
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

    fun chordEventsFlow(songId: String): Flow<List<ChordEvent>> =
        stateFlow.map { state ->
            state.chordEvents
                .filter { it.songId == songId }
                .sortedWith(compareBy<ChordEvent> { it.timestampMs }.thenBy { it.id })
        }

    fun practiceProfileFlow(songId: String): Flow<PracticeProfile> =
        stateFlow.map { state ->
            state.practiceProfiles.firstOrNull { it.songId == songId }
                ?: PracticeProfile(songId = songId)
        }

    suspend fun addSong(title: String, artist: String?, audioUri: Uri): Song {
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
            artist = artist?.trim().orEmpty().ifBlank { null },
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

    suspend fun updateSong(songId: String, title: String, artist: String?): Song {
        val state = stateFlow.value
        val existing = state.songs.firstOrNull { it.id == songId }
            ?: throw IllegalArgumentException("Song not found")
        val error = LibraryValidation.validateSongInput(title, existing.audioFilePath)
        require(error == null) { error ?: "Invalid song input" }

        val updated = existing.copy(
            title = title.trim(),
            artist = artist?.trim().orEmpty().ifBlank { null },
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
                practiceProfiles = state.practiceProfiles.filterNot { it.songId == songId }
            )
        }

        runCatching {
            val file = File(song.audioFilePath)
            if (file.exists()) file.delete()
        }.onFailure { Log.w(TAG, "Failed to delete audio file for ${song.id}", it) }
    }

    suspend fun addChordEvent(songId: String, timestampMs: Long, chordName: String, note: String?): ChordEvent {
        val error = LibraryValidation.validateChordEvent(timestampMs, chordName)
        require(error == null) { error ?: "Invalid chord event" }
        ensureSongExists(songId)

        val event = ChordEvent(
            id = UUID.randomUUID().toString(),
            songId = songId,
            timestampMs = timestampMs,
            chordName = chordName.trim(),
            note = note?.trim().orEmpty().ifBlank { null }
        )

        mutateState { state ->
            state.copy(chordEvents = state.chordEvents + event)
        }

        return event
    }

    suspend fun updateChordEvent(event: ChordEvent): ChordEvent {
        val error = LibraryValidation.validateChordEvent(event.timestampMs, event.chordName)
        require(error == null) { error ?: "Invalid chord event" }
        ensureSongExists(event.songId)

        mutateState { state ->
            val exists = state.chordEvents.any { it.id == event.id }
            require(exists) { "Chord event not found" }
            state.copy(chordEvents = state.chordEvents.map { if (it.id == event.id) event else it })
        }

        return event
    }

    suspend fun deleteChordEvent(eventId: String) {
        mutateState { state ->
            state.copy(chordEvents = state.chordEvents.filterNot { it.id == eventId })
        }
    }

    suspend fun upsertPracticeProfile(songId: String, profile: PlaybackPracticeConfig) {
        val song = stateFlow.value.songs.firstOrNull { it.id == songId }
            ?: throw IllegalArgumentException("Song not found")
        val validationError = LibraryValidation.validatePracticeConfig(profile, song.durationMs)
        require(validationError == null) { validationError ?: "Invalid practice config" }

        val saved = PracticeProfile(
            songId = songId,
            startSpeed = profile.startSpeed,
            stepSize = profile.stepSize,
            targetSpeed = profile.targetSpeed,
            loopsPerSpeed = profile.loopsPerSpeed,
            loopEnabled = profile.loopEnabled,
            loopStartMs = profile.loopStartMs,
            loopEndMs = profile.loopEndMs
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

    companion object {
        private const val TAG = "SongRepository"
        val SUPPORTED_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg")
    }
}

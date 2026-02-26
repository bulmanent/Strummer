package com.strummer.practice.ui.songs

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strummer.practice.audio.PlaybackService
import com.strummer.practice.data.SettingsRepository
import com.strummer.practice.library.ActiveChordCue
import com.strummer.practice.library.ChordEvent
import com.strummer.practice.library.ChordTimelineService
import com.strummer.practice.library.AudioFileStatus
import com.strummer.practice.library.LibraryValidation
import com.strummer.practice.library.PlaybackPracticeConfig
import com.strummer.practice.library.PracticeProfile
import com.strummer.practice.library.Song
import com.strummer.practice.repo.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val selectedSongId: String? = null,
    val selectedSongTitleInput: String = "",
    val selectedSongArtistInput: String = "",
    val importTitleInput: String = "",
    val importArtistInput: String = "",
    val chordEvents: List<ChordEvent> = emptyList(),
    val currentCue: ChordEvent? = null,
    val nextCue: ChordEvent? = null,
    val timeToNextMs: Long? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val steppedModeEnabled: Boolean = false,
    val steppedStartSpeed: Float = 0.6f,
    val steppedStepSize: Float = 0.05f,
    val steppedTargetSpeed: Float = 1.0f,
    val steppedLoopsPerSpeed: Int = 1,
    val loopEnabled: Boolean = false,
    val loopStartMs: Long? = null,
    val loopEndMs: Long? = null,
    val nextSteppedSpeed: Float? = null,
    val pitchStatus: String = "Pitch correction active",
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val missingFileMessage: String? = null,
    val selectedEventId: String? = null
) {
    val selectedSong: Song?
        get() = songs.firstOrNull { it.id == selectedSongId }
}

class SongsViewModel(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackService: PlaybackService,
    private val timelineService: ChordTimelineService
) : ViewModel() {
    private var chordEventsJob: Job? = null
    private var practiceProfileJob: Job? = null
    private var observedSongId: String? = null

    private val _uiState = MutableStateFlow(SongsUiState())
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    init {
        observeSongs()
        observePlayback()
    }

    private fun observeSongs() {
        viewModelScope.launch {
            val lastSongTitle = settingsRepository.settingsFlow.first().lastSongTitle
            songRepository.songsFlow().collectLatest { songs ->
                val currentSelected = _uiState.value.selectedSongId
                val resolvedId = when {
                    currentSelected != null && songs.any { it.id == currentSelected } -> currentSelected
                    lastSongTitle.isNotBlank() -> songs.firstOrNull { it.title == lastSongTitle }?.id
                    else -> songs.firstOrNull()?.id
                }

                _uiState.value = _uiState.value.copy(
                    songs = songs,
                    selectedSongId = resolvedId,
                    selectedSongTitleInput = songs.firstOrNull { it.id == resolvedId }?.title.orEmpty(),
                    selectedSongArtistInput = songs.firstOrNull { it.id == resolvedId }?.artist.orEmpty()
                )

                if (resolvedId != null) {
                    observeSongSpecificFlows(resolvedId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        chordEvents = emptyList(),
                        currentCue = null,
                        nextCue = null,
                        timeToNextMs = null,
                        missingFileMessage = null
                    )
                }
            }
        }
    }

    private fun observeSongSpecificFlows(songId: String) {
        if (observedSongId == songId) return
        observedSongId = songId
        chordEventsJob?.cancel()
        practiceProfileJob?.cancel()

        chordEventsJob = viewModelScope.launch {
            songRepository.chordEventsFlow(songId).collect { events ->
                _uiState.value = _uiState.value.copy(chordEvents = events)
                recalculateCue(_uiState.value.positionMs)
            }
        }

        practiceProfileJob = viewModelScope.launch {
            songRepository.practiceProfileFlow(songId).collect { profile ->
                applyProfile(profile)
            }
        }

        val selectedSong = _uiState.value.songs.firstOrNull { it.id == songId } ?: return
        when (val status = songRepository.audioFileStatus(selectedSong)) {
            AudioFileStatus.Available -> {
                _uiState.value = _uiState.value.copy(missingFileMessage = null)
                playbackService.load(selectedSong.audioFilePath)
            }

            is AudioFileStatus.Missing -> {
                _uiState.value = _uiState.value.copy(missingFileMessage = status.message)
            }
        }

        viewModelScope.launch {
            settingsRepository.updateSongSelection(selectedSong.title, "")
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            combine(
                playbackService.isPlaying,
                playbackService.positionMs,
                playbackService.durationMs,
                playbackService.speed
            ) { isPlaying, position, duration, speed ->
                PlaybackUi(isPlaying, position, duration, speed)
            }.collect { playback ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = playback.isPlaying,
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs,
                    speed = playback.speed
                )
                recalculateCue(playback.positionMs)
            }
        }

        viewModelScope.launch {
            playbackService.nextSteppedSpeed.collect { value ->
                _uiState.value = _uiState.value.copy(nextSteppedSpeed = value)
            }
        }
        viewModelScope.launch {
            playbackService.pitchStatus.collect { value ->
                _uiState.value = _uiState.value.copy(pitchStatus = value)
            }
        }
        viewModelScope.launch {
            playbackService.error.collect { value ->
                _uiState.value = _uiState.value.copy(errorMessage = value)
            }
        }
    }

    fun selectSong(songId: String) {
        _uiState.value = _uiState.value.copy(selectedSongId = songId, infoMessage = null, errorMessage = null)
        observeSongSpecificFlows(songId)
    }

    fun setImportTitle(value: String) {
        _uiState.value = _uiState.value.copy(importTitleInput = value)
    }

    fun setImportArtist(value: String) {
        _uiState.value = _uiState.value.copy(importArtistInput = value)
    }

    fun setSelectedSongTitle(value: String) {
        _uiState.value = _uiState.value.copy(selectedSongTitleInput = value)
    }

    fun setSelectedSongArtist(value: String) {
        _uiState.value = _uiState.value.copy(selectedSongArtistInput = value)
    }

    fun importSong(uri: Uri) {
        val state = _uiState.value
        val title = state.importTitleInput.trim()
        val artist = state.importArtistInput.trim().ifBlank { null }

        val validation = LibraryValidation.validateSongInput(title, "pending")
        if (validation != null) {
            _uiState.value = state.copy(errorMessage = validation)
            return
        }

        viewModelScope.launch {
            runCatching {
                songRepository.addSong(title = title, artist = artist, audioUri = uri)
            }.onSuccess { song ->
                _uiState.value = _uiState.value.copy(
                    importTitleInput = "",
                    importArtistInput = "",
                    selectedSongId = song.id,
                    selectedSongTitleInput = song.title,
                    selectedSongArtistInput = song.artist.orEmpty(),
                    infoMessage = "Imported ${song.title}",
                    errorMessage = null
                )
                observeSongSpecificFlows(song.id)
            }.onFailure { err ->
                Log.e(TAG, "Import failed", err)
                _uiState.value = _uiState.value.copy(errorMessage = err.message ?: "Import failed")
            }
        }
    }

    fun saveSongEdits() {
        val state = _uiState.value
        val songId = state.selectedSongId ?: return

        viewModelScope.launch {
            runCatching {
                songRepository.updateSong(songId, state.selectedSongTitleInput, state.selectedSongArtistInput)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(infoMessage = "Song updated", errorMessage = null)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Failed to update song")
            }
        }
    }

    fun deleteSelectedSong() {
        val songId = _uiState.value.selectedSongId ?: return
        viewModelScope.launch {
            songRepository.deleteSong(songId)
            _uiState.value = _uiState.value.copy(
                selectedSongId = null,
                selectedSongTitleInput = "",
                selectedSongArtistInput = "",
                infoMessage = "Song deleted"
            )
        }
    }

    fun playPause() {
        if (_uiState.value.isPlaying) {
            playbackService.pause()
        } else {
            playbackService.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playbackService.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(steppedModeEnabled = false)
        playbackService.disableSteppedMode()
        playbackService.setSpeed(speed)
    }

    fun addChordAtCurrentTime(chordName: String, note: String?) {
        val songId = _uiState.value.selectedSongId ?: return
        val timestamp = _uiState.value.positionMs
        addChord(songId, timestamp, chordName, note)
    }

    fun addChordAtTimestamp(timestampMs: Long, chordName: String, note: String?) {
        val songId = _uiState.value.selectedSongId ?: return
        addChord(songId, timestampMs, chordName, note)
    }

    private fun addChord(songId: String, timestampMs: Long, chordName: String, note: String?) {
        viewModelScope.launch {
            runCatching {
                songRepository.addChordEvent(songId, timestampMs, chordName, note)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = "Chord added")
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Failed to add chord")
            }
        }
    }

    fun updateChordEvent(eventId: String, timestampMs: Long, chordName: String, note: String?) {
        val state = _uiState.value
        val existing = state.chordEvents.firstOrNull { it.id == eventId } ?: return
        val updated = existing.copy(
            timestampMs = timestampMs,
            chordName = chordName.trim(),
            note = note?.trim().orEmpty().ifBlank { null }
        )

        viewModelScope.launch {
            runCatching { songRepository.updateChordEvent(updated) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Failed to update chord")
                }
        }
    }

    fun deleteChordEvent(eventId: String) {
        viewModelScope.launch {
            songRepository.deleteChordEvent(eventId)
            _uiState.value = _uiState.value.copy(infoMessage = "Chord deleted")
        }
    }

    fun selectEvent(eventId: String?) {
        _uiState.value = _uiState.value.copy(selectedEventId = eventId)
    }

    fun setSteppedStartSpeed(value: Float) {
        _uiState.value = _uiState.value.copy(steppedStartSpeed = value)
        persistPracticeProfile()
    }

    fun setSteppedStepSize(value: Float) {
        _uiState.value = _uiState.value.copy(steppedStepSize = value)
        persistPracticeProfile()
    }

    fun setSteppedTargetSpeed(value: Float) {
        _uiState.value = _uiState.value.copy(steppedTargetSpeed = value)
        persistPracticeProfile()
    }

    fun setSteppedLoopsPerSpeed(value: Int) {
        _uiState.value = _uiState.value.copy(steppedLoopsPerSpeed = value)
        persistPracticeProfile()
    }

    fun setLoopEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(loopEnabled = enabled)
        persistPracticeProfile()
    }

    fun setLoopStartMs(value: Long?) {
        _uiState.value = _uiState.value.copy(loopStartMs = value)
        persistPracticeProfile()
    }

    fun setLoopEndMs(value: Long?) {
        _uiState.value = _uiState.value.copy(loopEndMs = value)
        persistPracticeProfile()
    }

    fun setSteppedModeEnabled(enabled: Boolean) {
        val state = _uiState.value
        _uiState.value = state.copy(steppedModeEnabled = enabled)
        if (!enabled) {
            playbackService.disableSteppedMode()
            return
        }

        val song = state.selectedSong ?: return
        val config = state.asPracticeConfig()
        val validation = LibraryValidation.validatePracticeConfig(config, song.durationMs)
        if (validation != null) {
            _uiState.value = state.copy(errorMessage = validation, steppedModeEnabled = false)
            return
        }

        playbackService.enableSteppedMode(config)
    }

    fun resetSteppedMode() {
        playbackService.resetSteppedMode()
    }

    private fun applyProfile(profile: PracticeProfile) {
        _uiState.value = _uiState.value.copy(
            steppedStartSpeed = profile.startSpeed,
            steppedStepSize = profile.stepSize,
            steppedTargetSpeed = profile.targetSpeed,
            steppedLoopsPerSpeed = profile.loopsPerSpeed,
            loopEnabled = profile.loopEnabled,
            loopStartMs = profile.loopStartMs,
            loopEndMs = profile.loopEndMs
        )
    }

    private fun persistPracticeProfile() {
        val state = _uiState.value
        val songId = state.selectedSongId ?: return
        viewModelScope.launch {
            runCatching {
                songRepository.upsertPracticeProfile(songId, state.asPracticeConfig())
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Invalid practice settings")
            }
        }
    }

    private fun recalculateCue(positionMs: Long) {
        val events = _uiState.value.chordEvents
        val cue: ActiveChordCue = timelineService.cueAt(events, positionMs)
        val timeToNext = cue.next?.timestampMs?.minus(positionMs)?.coerceAtLeast(0L)
        _uiState.value = _uiState.value.copy(
            currentCue = cue.current,
            nextCue = cue.next,
            timeToNextMs = timeToNext
        )
    }

    override fun onCleared() {
        super.onCleared()
        chordEventsJob?.cancel()
        practiceProfileJob?.cancel()
        playbackService.release()
    }

    private fun SongsUiState.asPracticeConfig(): PlaybackPracticeConfig {
        return PlaybackPracticeConfig(
            startSpeed = steppedStartSpeed,
            stepSize = steppedStepSize,
            targetSpeed = steppedTargetSpeed,
            loopsPerSpeed = steppedLoopsPerSpeed,
            loopEnabled = loopEnabled,
            loopStartMs = loopStartMs,
            loopEndMs = loopEndMs
        )
    }

    companion object {
        private const val TAG = "SongsViewModel"

        fun factory(
            songRepository: SongRepository,
            settingsRepository: SettingsRepository,
            playbackService: PlaybackService,
            timelineService: ChordTimelineService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongsViewModel(songRepository, settingsRepository, playbackService, timelineService) as T
            }
        }
    }

    private data class PlaybackUi(
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val speed: Float
    )
}

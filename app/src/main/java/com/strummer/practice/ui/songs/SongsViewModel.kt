package com.strummer.practice.ui.songs

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strummer.practice.audio.PlaybackService
import com.strummer.practice.data.SettingsRepository
import com.strummer.practice.library.AudioFileStatus
import com.strummer.practice.library.BarChordStep
import com.strummer.practice.library.BarLoopTimelineService
import com.strummer.practice.library.PracticeProfile
import com.strummer.practice.library.Song
import com.strummer.practice.repo.SongRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val selectedSongId: String? = null,
    val songTitleInput: String = "",
    val barSteps: List<BarChordStep> = emptyList(),
    val tempoBpm: Int = 100,
    val timeSignatureTop: Int = 4,
    val timeSignatureBottom: Int = 4,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val isPlaying: Boolean = false,
    val currentChord: String = "-",
    val nextChord: String = "-",
    val absoluteBar: Int = 1,
    val loopBar: Int = 1,
    val barsUntilNextChange: Int = 1,
    val pitchStatus: String = "Pitch correction active",
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val missingFileMessage: String? = null
) {
    val selectedSong: Song?
        get() = songs.firstOrNull { it.id == selectedSongId }

    val totalLoopBars: Int
        get() = barSteps.sumOf { it.barCount }.coerceAtLeast(0)
}

class SongsViewModel(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackService: PlaybackService,
    private val barLoopTimelineService: BarLoopTimelineService
) : ViewModel() {
    private var barStepsJob: Job? = null
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

                val selectedSong = songs.firstOrNull { it.id == resolvedId }
                _uiState.value = _uiState.value.copy(
                    songs = songs,
                    selectedSongId = resolvedId,
                    songTitleInput = selectedSong?.title.orEmpty()
                )

                if (resolvedId != null) {
                    observeSongSpecificFlows(resolvedId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        barSteps = emptyList(),
                        currentChord = "-",
                        nextChord = "-",
                        missingFileMessage = null
                    )
                }
            }
        }
    }

    private fun observeSongSpecificFlows(songId: String) {
        if (observedSongId == songId) return
        observedSongId = songId
        barStepsJob?.cancel()
        practiceProfileJob?.cancel()

        barStepsJob = viewModelScope.launch {
            songRepository.barStepsFlow(songId).collect { steps ->
                _uiState.value = _uiState.value.copy(barSteps = steps)
                recalculateBarCue(_uiState.value.positionMs)
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
                recalculateBarCue(playback.positionMs)
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

    fun setSongTitleInput(value: String) {
        _uiState.value = _uiState.value.copy(songTitleInput = value)
    }

    fun addSong(uri: Uri) {
        val title = _uiState.value.songTitleInput.trim()
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Song title is required")
            return
        }

        viewModelScope.launch {
            runCatching {
                songRepository.addSong(title = title, audioUri = uri)
            }.onSuccess { song ->
                _uiState.value = _uiState.value.copy(
                    selectedSongId = song.id,
                    infoMessage = "Added ${song.title}",
                    errorMessage = null
                )
                observeSongSpecificFlows(song.id)
            }.onFailure { err ->
                Log.e(TAG, "Import failed", err)
                _uiState.value = _uiState.value.copy(errorMessage = err.message ?: "Import failed")
            }
        }
    }

    fun selectSong(songId: String) {
        _uiState.value = _uiState.value.copy(selectedSongId = songId, infoMessage = null, errorMessage = null)
        observeSongSpecificFlows(songId)
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
        playbackService.setSpeed(speed)
    }

    fun setTempoBpm(value: Int) {
        val safe = value.coerceIn(40, 220)
        _uiState.value = _uiState.value.copy(tempoBpm = safe)
        persistProfile()
        recalculateBarCue(_uiState.value.positionMs)
    }

    fun setTimeSignatureTop(value: Int) {
        val safe = value.coerceIn(2, 12)
        _uiState.value = _uiState.value.copy(timeSignatureTop = safe)
        persistProfile()
        recalculateBarCue(_uiState.value.positionMs)
    }

    fun addStep(chordName: String, barCount: Int) {
        val songId = _uiState.value.selectedSongId ?: return
        viewModelScope.launch {
            runCatching {
                songRepository.addBarStep(songId = songId, chordName = chordName, barCount = barCount)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Failed to add step")
            }
        }
    }

    fun updateStep(stepId: String, chordName: String, barCount: Int) {
        val existing = _uiState.value.barSteps.firstOrNull { it.id == stepId } ?: return
        val updated = existing.copy(chordName = chordName.trim(), barCount = barCount)

        viewModelScope.launch {
            runCatching { songRepository.updateBarStep(updated) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Failed to update step")
                }
        }
    }

    fun deleteStep(stepId: String) {
        viewModelScope.launch {
            songRepository.deleteBarStep(stepId)
        }
    }

    fun deleteSelectedSong() {
        val songId = _uiState.value.selectedSongId ?: return
        viewModelScope.launch {
            songRepository.deleteSong(songId)
            _uiState.value = _uiState.value.copy(
                selectedSongId = null,
                songTitleInput = "",
                infoMessage = "Song deleted",
                barSteps = emptyList(),
                currentChord = "-",
                nextChord = "-"
            )
        }
    }

    private fun applyProfile(profile: PracticeProfile) {
        _uiState.value = _uiState.value.copy(
            tempoBpm = profile.tempoBpm,
            timeSignatureTop = profile.timeSignatureTop,
            timeSignatureBottom = profile.timeSignatureBottom
        )
    }

    private fun persistProfile() {
        val state = _uiState.value
        val songId = state.selectedSongId ?: return
        viewModelScope.launch {
            runCatching {
                songRepository.upsertPracticeProfile(
                    songId = songId,
                    tempoBpm = state.tempoBpm,
                    timeSignatureTop = state.timeSignatureTop,
                    timeSignatureBottom = state.timeSignatureBottom
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Invalid practice settings")
            }
        }
    }

    private fun recalculateBarCue(positionMs: Long) {
        val state = _uiState.value
        val resolved = barLoopTimelineService.resolve(
            elapsedMs = positionMs,
            tempoBpm = state.tempoBpm,
            timeSignatureTop = state.timeSignatureTop,
            steps = state.barSteps
        )

        if (resolved == null) {
            _uiState.value = state.copy(
                currentChord = "-",
                nextChord = "-",
                absoluteBar = 1,
                loopBar = 1,
                barsUntilNextChange = 1
            )
            return
        }

        _uiState.value = state.copy(
            currentChord = resolved.currentChord,
            nextChord = resolved.nextChord,
            absoluteBar = resolved.absoluteBar,
            loopBar = resolved.loopBar,
            barsUntilNextChange = resolved.barsUntilNextChange
        )
    }

    override fun onCleared() {
        super.onCleared()
        barStepsJob?.cancel()
        practiceProfileJob?.cancel()
        playbackService.release()
    }

    companion object {
        private const val TAG = "SongsViewModel"

        fun factory(
            songRepository: SongRepository,
            settingsRepository: SettingsRepository,
            playbackService: PlaybackService,
            barLoopTimelineService: BarLoopTimelineService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongsViewModel(
                    songRepository = songRepository,
                    settingsRepository = settingsRepository,
                    playbackService = playbackService,
                    barLoopTimelineService = barLoopTimelineService
                ) as T
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

package com.strummer.practice.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strummer.practice.audio.PracticeAudioEngine
import com.strummer.practice.data.SettingsRepository
import com.strummer.practice.model.PlaybackBar
import com.strummer.practice.model.RampConfig
import com.strummer.practice.model.Section
import com.strummer.practice.model.Song
import com.strummer.practice.model.StrumPattern
import com.strummer.practice.repo.AssetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val patterns: List<StrumPattern> = emptyList(),
    val selectedSongIndex: Int = 0,
    val selectedSectionIndex: Int = 0,
    val tempoBpm: Int = 80,
    val rampEnabled: Boolean = false,
    val rampStartBpm: Int = 60,
    val rampEndBpm: Int = 100,
    val rampIncrement: Int = 2,
    val barsPerIncrement: Int = 4,
    val isPlaying: Boolean = false,
    val currentBarIndex: Int = 0,
    val currentStepIndex: Int = 0,
    val currentChord: String = "",
    val effectiveBpm: Int = 80,
    val rampBarsUntilIncrement: Int = 0
) {
    val selectedSong: Song?
        get() = songs.getOrNull(selectedSongIndex)

    val selectedSection: Section?
        get() = selectedSong?.sections?.getOrNull(selectedSectionIndex)
}

class SongsViewModel(
    private val assetRepository: AssetRepository,
    private val settingsRepository: SettingsRepository,
    private val audioEngine: PracticeAudioEngine
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongsUiState())
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    init {
        observeEngine()
        loadData()
    }

    private fun observeEngine() {
        viewModelScope.launch {
            combine(
                audioEngine.isPlaying,
                audioEngine.currentBarIndex,
                audioEngine.currentStepIndex,
                audioEngine.currentChord,
                audioEngine.currentBpm
            ) { playing, bar, step, chord, bpm ->
                EngineUi(playing, bar, step, chord, bpm, _uiState.value.rampBarsUntilIncrement)
            }.collect { ui ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = ui.isPlaying,
                    currentBarIndex = ui.barIndex,
                    currentStepIndex = ui.stepIndex,
                    currentChord = ui.chord,
                    effectiveBpm = ui.bpm,
                    rampBarsUntilIncrement = ui.barsUntilIncrement
                )
            }
        }

        viewModelScope.launch {
            audioEngine.rampProgress.collect { ramp ->
                _uiState.value = _uiState.value.copy(rampBarsUntilIncrement = ramp.barsUntilIncrement)
            }
        }

        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    tempoBpm = settings.tempoBpm,
                    rampEnabled = settings.rampEnabled,
                    rampStartBpm = settings.rampStartBpm,
                    rampEndBpm = settings.rampEndBpm,
                    rampIncrement = settings.rampIncrement,
                    barsPerIncrement = settings.barsPerIncrement
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val songs = assetRepository.loadSongs()
            val patterns = assetRepository.loadPatterns()
            val settings = settingsRepository.settingsFlow.first()
            val songIndex = songs.indexOfFirst { it.title == settings.lastSongTitle }.takeIf { it >= 0 } ?: 0
            val sectionIndex = songs
                .getOrNull(songIndex)
                ?.sections
                ?.indexOfFirst { it.name == settings.lastSectionName }
                ?.takeIf { it >= 0 }
                ?: 0
            _uiState.value = _uiState.value.copy(
                songs = songs,
                patterns = patterns,
                selectedSongIndex = songIndex,
                selectedSectionIndex = sectionIndex
            )
        }
    }

    fun selectSong(index: Int) {
        val safeIndex = index.coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(selectedSongIndex = safeIndex, selectedSectionIndex = 0)
    }

    fun selectSection(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSectionIndex = index.coerceAtLeast(0))
    }

    fun setTempo(bpm: Int) {
        val value = bpm.coerceIn(40, 160)
        _uiState.value = _uiState.value.copy(tempoBpm = value)
        viewModelScope.launch { settingsRepository.updateTempo(value) }
        if (_uiState.value.isPlaying) {
            audioEngine.setBpm(value)
        }
    }

    fun adjustTempo(delta: Int) = setTempo(_uiState.value.tempoBpm + delta)

    fun setRampEnabled(enabled: Boolean) {
        val current = _uiState.value
        _uiState.value = current.copy(rampEnabled = enabled)
        persistRamp()
        if (current.isPlaying) audioEngine.setRamp(buildRampConfig())
    }

    fun setRampStart(value: Int) {
        _uiState.value = _uiState.value.copy(rampStartBpm = value.coerceIn(40, 160))
        persistRamp()
    }

    fun setRampEnd(value: Int) {
        _uiState.value = _uiState.value.copy(rampEndBpm = value.coerceIn(40, 160))
        persistRamp()
    }

    fun setRampIncrement(value: Int) {
        _uiState.value = _uiState.value.copy(rampIncrement = value.coerceAtLeast(1))
        persistRamp()
    }

    fun setBarsPerIncrement(value: Int) {
        _uiState.value = _uiState.value.copy(barsPerIncrement = value.coerceAtLeast(1))
        persistRamp()
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            audioEngine.stop()
            return
        }

        val state = _uiState.value
        val section = state.selectedSection ?: return

        val patternsById = state.patterns.associateBy { it.id }
        val bars = section.bars.mapNotNull { bar ->
            patternsById[bar.patternId]?.let { pattern ->
                PlaybackBar(chord = bar.chord, beatsPerBar = bar.beatsPerBar, pattern = pattern)
            }
        }

        if (bars.isEmpty()) return

        viewModelScope.launch {
            settingsRepository.updateSongSelection(
                state.selectedSong?.title.orEmpty(),
                section.name
            )
        }
        audioEngine.start(state.tempoBpm, bars, buildRampConfig())
    }

    private fun persistRamp() {
        val state = _uiState.value
        viewModelScope.launch {
            settingsRepository.updateRamp(
                enabled = state.rampEnabled,
                start = state.rampStartBpm,
                end = state.rampEndBpm,
                increment = state.rampIncrement,
                bars = state.barsPerIncrement
            )
        }
    }

    private fun buildRampConfig(): RampConfig {
        val s = _uiState.value
        return RampConfig(
            enabled = s.rampEnabled,
            startBpm = s.rampStartBpm,
            endBpm = s.rampEndBpm,
            increment = s.rampIncrement,
            barsPerIncrement = s.barsPerIncrement
        )
    }

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        fun factory(
            assetRepository: AssetRepository,
            settingsRepository: SettingsRepository,
            audioEngine: PracticeAudioEngine
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongsViewModel(assetRepository, settingsRepository, audioEngine) as T
            }
        }
    }

    private data class EngineUi(
        val isPlaying: Boolean,
        val barIndex: Int,
        val stepIndex: Int,
        val chord: String,
        val bpm: Int,
        val barsUntilIncrement: Int
    )
}

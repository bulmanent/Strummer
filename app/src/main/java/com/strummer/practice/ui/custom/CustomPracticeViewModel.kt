package com.strummer.practice.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strummer.practice.audio.PracticeAudioEngine
import com.strummer.practice.data.SettingsRepository
import com.strummer.practice.model.PlaybackBar
import com.strummer.practice.model.RampConfig
import com.strummer.practice.model.StrumPattern
import com.strummer.practice.repo.AssetRepository
import com.strummer.practice.util.PatternDslParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CustomPracticeUiState(
    val availablePatterns: List<StrumPattern> = emptyList(),
    val selectedPatternId: String = "",
    val patternDsl: String = "D D U U D U",
    val chordSequenceInput: String = "A C D G",
    val beatsPerBar: Int = 4,
    val tempoBpm: Int = 80,
    val isPlaying: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentChord: String = "",
    val errorMessage: String = ""
)

class CustomPracticeViewModel(
    private val assetRepository: AssetRepository,
    private val settingsRepository: SettingsRepository,
    private val audioEngine: PracticeAudioEngine
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomPracticeUiState())
    val uiState: StateFlow<CustomPracticeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val patterns = assetRepository.loadPatterns()
            val settings = settingsRepository.settingsFlow.first()
            val restoredId = settings.lastPatternId
            _uiState.value = _uiState.value.copy(
                availablePatterns = patterns,
                selectedPatternId = patterns.firstOrNull { it.id == restoredId }?.id
                    ?: patterns.firstOrNull()?.id.orEmpty()
            )
        }

        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    tempoBpm = settings.tempoBpm,
                    chordSequenceInput = settings.customChordSequence,
                    patternDsl = settings.customPatternDsl
                )
            }
        }

        viewModelScope.launch {
            combine(
                audioEngine.isPlaying,
                audioEngine.currentStepIndex,
                audioEngine.currentChord
            ) { playing, step, chord ->
                Triple(playing, step, chord)
            }.collect { (playing, step, chord) ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = playing,
                    currentStepIndex = step,
                    currentChord = chord
                )
            }
        }
    }

    fun setPatternId(id: String) {
        _uiState.value = _uiState.value.copy(selectedPatternId = id)
        viewModelScope.launch { settingsRepository.updatePatternSelection(id) }
    }

    fun setPatternDsl(text: String) {
        _uiState.value = _uiState.value.copy(patternDsl = text)
        viewModelScope.launch {
            settingsRepository.updateCustomPractice(
                _uiState.value.chordSequenceInput,
                text
            )
        }
    }

    fun setChordSequence(input: String) {
        _uiState.value = _uiState.value.copy(chordSequenceInput = input)
        viewModelScope.launch {
            settingsRepository.updateCustomPractice(
                input,
                _uiState.value.patternDsl
            )
        }
    }

    fun setBeatsPerBar(beats: Int) {
        _uiState.value = _uiState.value.copy(beatsPerBar = beats)
    }

    fun setTempo(bpm: Int) {
        val safe = bpm.coerceIn(40, 160)
        _uiState.value = _uiState.value.copy(tempoBpm = safe)
        viewModelScope.launch { settingsRepository.updateTempo(safe) }
        if (_uiState.value.isPlaying) audioEngine.setBpm(safe)
    }

    fun togglePlayback(useDslPattern: Boolean) {
        if (_uiState.value.isPlaying) {
            audioEngine.stop()
            _uiState.value = _uiState.value.copy(errorMessage = "")
            return
        }

        val state = _uiState.value
        val pattern = if (useDslPattern) {
            runCatching {
                PatternDslParser.parse(
                    id = "custom-dsl",
                    name = "Custom DSL",
                    subdivision = 8,
                    dsl = state.patternDsl
                )
            }.getOrElse {
                _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Invalid pattern DSL")
                return
            }
        } else {
            state.availablePatterns.firstOrNull { it.id == state.selectedPatternId }
                ?: return
        }

        val chords = state.chordSequenceInput
            .split(Regex("[,\\s]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (chords.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter at least one chord")
            return
        }

        val bars = chords.map { chord ->
            PlaybackBar(
                chord = chord,
                beatsPerBar = state.beatsPerBar,
                pattern = pattern
            )
        }

        audioEngine.start(
            initialBpm = state.tempoBpm,
            sequence = bars,
            ramp = RampConfig(enabled = false, startBpm = state.tempoBpm, endBpm = state.tempoBpm, increment = 1, barsPerIncrement = 1)
        )
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }

    companion object {
        fun factory(
            assetRepository: AssetRepository,
            settingsRepository: SettingsRepository,
            audioEngine: PracticeAudioEngine
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CustomPracticeViewModel(assetRepository, settingsRepository, audioEngine) as T
            }
        }
    }
}

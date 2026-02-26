package com.strummer.practice.audio

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import com.strummer.practice.library.PlaybackPracticeConfig
import com.strummer.practice.library.PracticeModeService
import com.strummer.practice.library.PracticeModeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackService(
    private val practiceModeService: PracticeModeService = PracticeModeService()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var steppedConfig: PlaybackPracticeConfig? = null
    private var steppedState: PracticeModeState? = null
    private var pitchPreserveSupported = true

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _nextSteppedSpeed = MutableStateFlow<Float?>(null)
    val nextSteppedSpeed: StateFlow<Float?> = _nextSteppedSpeed.asStateFlow()

    private val _pitchStatus = MutableStateFlow("Pitch correction active")
    val pitchStatus: StateFlow<String> = _pitchStatus.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(path: String) {
        stopAndReleasePlayer()
        _error.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
        steppedConfig = null
        steppedState = null
        _nextSteppedSpeed.value = null

        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    _isPlaying.value = false
                }
                prepare()
            }
            mediaPlayer = player
            _durationMs.value = player.duration.toLong().coerceAtLeast(0L)
            applySpeed(1.0f)
        }.onFailure { err ->
            Log.e(TAG, "Failed to load audio path=$path", err)
            _error.value = "Unable to load audio file. ${err.message ?: "Unknown error"}"
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        if (_isPlaying.value) return

        runCatching {
            player.start()
            _isPlaying.value = true
            startProgressLoop()
        }.onFailure { err ->
            Log.e(TAG, "Playback start failed", err)
            _error.value = "Failed to start playback"
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        runCatching {
            player.pause()
            _isPlaying.value = false
        }.onFailure { err ->
            Log.e(TAG, "Playback pause failed", err)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val safe = positionMs.coerceIn(0L, _durationMs.value)
        runCatching {
            player.seekTo(safe.toInt())
            _positionMs.value = safe
        }.onFailure {
            Log.w(TAG, "Seek failed", it)
        }
    }

    fun setSpeed(speed: Float) {
        steppedConfig = null
        steppedState = null
        _nextSteppedSpeed.value = null
        applySpeed(speed.coerceIn(MIN_SPEED, MAX_SPEED))
    }

    fun enableSteppedMode(config: PlaybackPracticeConfig) {
        steppedConfig = config
        steppedState = practiceModeService.initialState(config)
        val state = steppedState ?: return
        _nextSteppedSpeed.value = state.nextSpeed
        applySpeed(state.currentSpeed)
    }

    fun resetSteppedMode() {
        val config = steppedConfig ?: return
        steppedState = practiceModeService.reset(config)
        val state = steppedState ?: return
        _nextSteppedSpeed.value = state.nextSpeed
        applySpeed(state.currentSpeed)
    }

    fun disableSteppedMode() {
        steppedConfig = null
        steppedState = null
        _nextSteppedSpeed.value = null
    }

    fun release() {
        progressJob?.cancel()
        stopAndReleasePlayer()
        scope.cancel()
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val player = mediaPlayer ?: break
                _positionMs.value = player.currentPosition.toLong().coerceAtLeast(0L)
                handleLoopBoundary()
                delay(100L)
            }
        }
    }

    private fun handleLoopBoundary() {
        val config = steppedConfig ?: return
        if (!config.loopEnabled) return
        val player = mediaPlayer ?: return

        val loopStart = config.loopStartMs ?: return
        val loopEnd = config.loopEndMs ?: return
        if (player.currentPosition.toLong() < loopEnd) return

        player.seekTo(loopStart.toInt())
        _positionMs.value = loopStart

        val currentState = steppedState ?: return
        val next = practiceModeService.onLoopBoundary(config, currentState)
        steppedState = next
        _nextSteppedSpeed.value = next.nextSpeed
        applySpeed(next.currentSpeed)
    }

    private fun applySpeed(newSpeed: Float) {
        val player = mediaPlayer ?: return
        _speed.value = newSpeed
        runCatching {
            val params = player.playbackParams
                .setPitch(1.0f)
                .setSpeed(newSpeed)
            player.playbackParams = params
            if (!pitchPreserveSupported) {
                pitchPreserveSupported = true
                _pitchStatus.value = "Pitch correction active"
            }
        }.onFailure { err ->
            if (pitchPreserveSupported) {
                pitchPreserveSupported = false
                _pitchStatus.value = "Pitch correction unavailable on this device"
            }
            Log.w(TAG, "Unable to apply pitch-preserving speed", err)
            runCatching {
                player.playbackParams = player.playbackParams.setSpeed(newSpeed)
            }
        }
    }

    private fun stopAndReleasePlayer() {
        progressJob?.cancel()
        progressJob = null
        mediaPlayer?.run {
            runCatching { stop() }
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    companion object {
        private const val TAG = "PlaybackService"
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 1.25f
    }
}

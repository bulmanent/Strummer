package com.strummer.practice.audio

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.strummer.practice.model.PatternStep
import com.strummer.practice.model.PlaybackBar
import com.strummer.practice.model.RampConfig
import com.strummer.practice.model.RampProgress
import com.strummer.practice.model.StepKind
import com.strummer.practice.util.TimingMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class PracticeAudioEngine {
    private val sampleRate = 44_100
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var schedulerJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private var bars: List<PlaybackBar> = emptyList()
    private var bpm: Int = 80
    private var rampConfig: RampConfig = RampConfig(
        enabled = false,
        startBpm = 60,
        endBpm = 100,
        increment = 2,
        barsPerIncrement = 4
    )

    private val downbeatClick = generateClick(freqHz = 1800.0, durationMs = 20, amp = 0.75)
    private val beatClick = generateClick(freqHz = 1200.0, durationMs = 16, amp = 0.6)
    private val cueClick = generateClick(freqHz = 700.0, durationMs = 10, amp = 0.35)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBarIndex = MutableStateFlow(0)
    val currentBarIndex: StateFlow<Int> = _currentBarIndex.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _currentChord = MutableStateFlow("")
    val currentChord: StateFlow<String> = _currentChord.asStateFlow()

    private val _currentBpm = MutableStateFlow(80)
    val currentBpm: StateFlow<Int> = _currentBpm.asStateFlow()

    private val _rampProgress = MutableStateFlow(RampProgress())
    val rampProgress: StateFlow<RampProgress> = _rampProgress.asStateFlow()

    fun start(initialBpm: Int, sequence: List<PlaybackBar>, ramp: RampConfig) {
        if (sequence.isEmpty()) return
        stop()

        bars = sequence
        bpm = initialBpm.coerceIn(40, 160)
        rampConfig = ramp
        if (ramp.enabled) {
            bpm = ramp.startBpm.coerceIn(40, 160)
        }
        _currentBpm.value = bpm
        _isPlaying.value = true

        schedulerJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runSchedulerLoop()
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null

        audioTrack?.run {
            try {
                pause()
                flush()
                stop()
            } catch (_: IllegalStateException) {
                // ignored
            }
            release()
        }
        audioTrack = null
        _isPlaying.value = false
    }

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(40, 160)
        _currentBpm.value = bpm
    }

    fun setRamp(config: RampConfig) {
        rampConfig = config
        if (config.enabled) {
            bpm = config.startBpm.coerceIn(40, 160)
            _currentBpm.value = bpm
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun runSchedulerLoop() {
        val track = createAudioTrack()
        audioTrack = track
        track.play()

        var barIndex = 0
        var stepIndexInBar = 0
        var patternStepIndex = 0
        var barsSinceRampIncrease = 0

        while (scope.isActive && _isPlaying.value) {
            val bar = bars[barIndex % bars.size]
            val pattern = bar.pattern
            val expectedStepsPerBar = (bar.beatsPerBar * (pattern.subdivision / 4)).coerceAtLeast(1)
            val step = pattern.steps[patternStepIndex % pattern.steps.size]

            _currentBarIndex.value = barIndex % bars.size
            _currentStepIndex.value = patternStepIndex % pattern.steps.size
            _currentChord.value = bar.chord

            val framesPerStep = TimingMath.framesPerStep(sampleRate, bpm, pattern.subdivision)
            val pcm = buildStepPcm(
                frames = framesPerStep,
                step = step,
                stepInBar = stepIndexInBar,
                expectedStepsPerBar = expectedStepsPerBar,
                beatsPerBar = bar.beatsPerBar
            )

            val written = track.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) {
                continue
            }

            stepIndexInBar += 1
            patternStepIndex += 1

            if (stepIndexInBar >= expectedStepsPerBar) {
                stepIndexInBar = 0
                patternStepIndex = 0
                barIndex = (barIndex + 1) % bars.size
                barsSinceRampIncrease += 1

                if (rampConfig.enabled) {
                    val barsUntil = (rampConfig.barsPerIncrement - barsSinceRampIncrease).coerceAtLeast(0)
                    _rampProgress.value = RampProgress(
                        active = true,
                        currentBpm = bpm,
                        barsUntilIncrement = barsUntil
                    )
                    if (barsSinceRampIncrease >= rampConfig.barsPerIncrement) {
                        barsSinceRampIncrease = 0
                        val target = (bpm + rampConfig.increment).coerceAtMost(rampConfig.endBpm)
                        bpm = target
                        _currentBpm.value = target
                    }
                } else {
                    _rampProgress.value = RampProgress(active = false, currentBpm = bpm, barsUntilIncrement = 0)
                }
            }
        }
    }

    private fun buildStepPcm(
        frames: Int,
        step: PatternStep,
        stepInBar: Int,
        expectedStepsPerBar: Int,
        beatsPerBar: Int
    ): ShortArray {
        val out = ShortArray(frames)

        if (TimingMath.isBeatBoundary(stepInBar, expectedStepsPerBar, beatsPerBar)) {
            if (TimingMath.isDownbeat(stepInBar)) {
                mixIn(out, downbeatClick)
            } else {
                mixIn(out, beatClick)
            }
        }

        if (step.kind != StepKind.Rest) {
            mixIn(out, cueClick)
        }

        return out
    }

    private fun createAudioTrack(): AudioTrack {
        val minBufferBytes = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
        val lookaheadBytes = (sampleRate * 0.20 * 2).toInt()
        val bufferSize = maxOf(minBufferBytes, lookaheadBytes)

        return AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .setChannelMask(channelConfig)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private fun mixIn(target: ShortArray, sample: ShortArray) {
        val size = minOf(target.size, sample.size)
        for (i in 0 until size) {
            val mixed = target[i].toInt() + sample[i].toInt()
            target[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun generateClick(freqHz: Double, durationMs: Int, amp: Double): ShortArray {
        val samples = ((durationMs / 1000.0) * sampleRate).toInt().coerceAtLeast(1)
        return ShortArray(samples) { idx ->
            val t = idx.toDouble() / sampleRate
            val envelope = 1.0 - (idx.toDouble() / samples)
            val value = sin(2.0 * PI * freqHz * t) * envelope * amp
            (value * Short.MAX_VALUE).toInt().toShort()
        }
    }
}

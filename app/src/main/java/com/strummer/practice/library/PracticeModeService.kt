package com.strummer.practice.library

import kotlin.math.min

data class PracticeModeState(
    val currentSpeed: Float,
    val loopsAtCurrentSpeed: Int,
    val nextSpeed: Float,
    val reachedTarget: Boolean
)

class PracticeModeService {
    fun initialState(config: PlaybackPracticeConfig): PracticeModeState {
        val initial = config.startSpeed.coerceAtMost(config.targetSpeed)
        return PracticeModeState(
            currentSpeed = initial,
            loopsAtCurrentSpeed = 0,
            nextSpeed = min(initial + config.stepSize, config.targetSpeed),
            reachedTarget = initial >= config.targetSpeed
        )
    }

    fun onLoopBoundary(config: PlaybackPracticeConfig, state: PracticeModeState): PracticeModeState {
        if (state.reachedTarget) {
            return state.copy(loopsAtCurrentSpeed = state.loopsAtCurrentSpeed + 1)
        }

        val loopCount = state.loopsAtCurrentSpeed + 1
        if (loopCount < config.loopsPerSpeed) {
            return state.copy(loopsAtCurrentSpeed = loopCount)
        }

        val advanced = min(state.currentSpeed + config.stepSize, config.targetSpeed)
        val reachedTarget = advanced >= config.targetSpeed
        return PracticeModeState(
            currentSpeed = advanced,
            loopsAtCurrentSpeed = 0,
            nextSpeed = min(advanced + config.stepSize, config.targetSpeed),
            reachedTarget = reachedTarget
        )
    }

    fun reset(config: PlaybackPracticeConfig): PracticeModeState = initialState(config)
}

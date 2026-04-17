package com.example.runapp

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class RaceType {
    WALK, RUN
}

enum class DistanceUnit(val factor: Double) {
    MILES(1609.34),
    METERS(1.0),
    KM(1000.0)
}

data class RaceSettings(
    val id: String = "default",
    val name: String = "My Race",
    val distanceValue: Double = 2.0,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val goalTime: Duration = 30.minutes,
    val walkDuration: Duration = 3.minutes,
    val runDuration: Duration = 1.minutes,
    val targetHeartRateWalk: Int = 120,
    val targetHeartRateRun: Int = 150
) {
    val distanceInMeters: Double get() = distanceValue * distanceUnit.factor
}

val DefaultProfiles = listOf(
    RaceSettings(id = "morning", name = "Morning Run", distanceValue = 2.0, goalTime = 20.minutes),
    RaceSettings(id = "5k", name = "5K Goal", distanceValue = 3.1, goalTime = 30.minutes),
    RaceSettings(id = "training", name = "Training", distanceValue = 5.0, goalTime = 60.minutes)
)

data class RaceState(
    val currentType: RaceType = RaceType.WALK,
    val sequenceStartTime: Duration = Duration.ZERO,
    val sequenceDuration: Duration = Duration.ZERO,
    val totalTime: Duration = Duration.ZERO,
    val totalDistance: Double = 0.0,
    val totalSteps: Int = 0,
    val currentHeartRate: Int = 0,
    val isPaused: Boolean = true,
    val isFinished: Boolean = false,
    val heartRateAlert: Boolean = false
) {
    val sequenceElapsedTime: Duration get() = totalTime - sequenceStartTime
    val sequenceRemainingTime: Duration get() = sequenceDuration - sequenceElapsedTime
    
    fun isOnTrack(settings: RaceSettings): String {
        if (totalTime.inWholeSeconds == 0L) return "Starting..."
        val expectedSpeed = settings.distanceInMeters / settings.goalTime.inWholeSeconds
        val currentSpeed = totalDistance / totalTime.inWholeSeconds
        
        return when {
            currentSpeed < expectedSpeed * 0.9 -> "TOO SLOW"
            currentSpeed > expectedSpeed * 1.1 -> "TOO FAST"
            else -> "ON TRACK"
        }
    }
}

class RaceEngine(
    private var settings: RaceSettings,
    private val onStateUpdate: (RaceState) -> Unit,
    private val onSequenceChange: (RaceType, Duration) -> Unit
) {
    private var state = RaceState()

    fun start() {
        state = state.copy(isPaused = false, sequenceDuration = settings.walkDuration)
        onStateUpdate(state)
        onSequenceChange(state.currentType, state.sequenceDuration)
    }

    fun pause() {
        state = state.copy(isPaused = true)
        onStateUpdate(state)
    }

    fun resume() {
        state = state.copy(isPaused = false)
        onStateUpdate(state)
    }

    fun updateTick(delta: Duration, distanceDelta: Double, stepsDelta: Int, heartRate: Int) {
        if (state.isPaused || state.isFinished) return

        val newTotalTime = state.totalTime + delta
        val newTotalDistance = state.totalDistance + distanceDelta
        val newTotalSteps = state.totalSteps + stepsDelta
        
        var newType = state.currentType
        var newSequenceStartTime = state.sequenceStartTime
        var newSequenceDuration = state.sequenceDuration

        if (newTotalTime - newSequenceStartTime >= newSequenceDuration) {
            // Switch sequence
            newType = if (state.currentType == RaceType.WALK) RaceType.RUN else RaceType.WALK
            newSequenceStartTime = newTotalTime
            newSequenceDuration = if (newType == RaceType.WALK) settings.walkDuration else settings.runDuration
            onSequenceChange(newType, newSequenceDuration)
        }

        val isFinished = newTotalDistance >= settings.distanceInMeters

        state = state.copy(
            currentType = newType,
            sequenceStartTime = newSequenceStartTime,
            sequenceDuration = newSequenceDuration,
            totalTime = newTotalTime,
            totalDistance = newTotalDistance,
            totalSteps = newTotalSteps,
            currentHeartRate = heartRate,
            isFinished = isFinished,
            heartRateAlert = heartRate > (if (newType == RaceType.WALK) settings.targetHeartRateWalk else settings.targetHeartRateRun)
        )
        
        onStateUpdate(state)
    }

    fun updateSettings(newSettings: RaceSettings) {
        settings = newSettings
    }
}

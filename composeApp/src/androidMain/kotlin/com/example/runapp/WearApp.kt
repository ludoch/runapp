package com.example.runapp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.platform.LocalContext

@Composable
fun WearApp() {
    val context = LocalContext.current
    
    // Load initial profiles (In a real app, this would be from a database/DataStore)
    var profiles by remember { mutableStateOf(DefaultProfiles) }
    var selectedProfileIndex by remember { mutableStateOf(0) }
    
    val currentSettings = profiles[selectedProfileIndex]
    
    var state by remember { mutableStateOf(RaceState()) }
    var isSettingsOpen by remember { mutableStateOf(true) }

    val engine = remember {
        RaceEngine(
            settings = currentSettings,
            onStateUpdate = { state = it },
            onSequenceChange = { _, _ -> }
        )
    }

    MaterialTheme {
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = rememberScalingLazyListState()) }
        ) {
            if (isSettingsOpen) {
                WearSettingsScreen(
                    settings = currentSettings,
                    onSettingsChange = { updated ->
                        // Update the profile in our list
                        val newList = profiles.toMutableList()
                        newList[selectedProfileIndex] = updated
                        profiles = newList
                        // In a real app: saveToDisk(updated)
                    },
                    onProfileCycle = {
                        selectedProfileIndex = (selectedProfileIndex + 1) % profiles.size
                    },
                    onStart = {
                        engine.updateSettings(profiles[selectedProfileIndex])
                        engine.start()
                        isSettingsOpen = false
                    }
                )
            } else {
                WearRaceScreen(
                    state = state,
                    settings = currentSettings,
                    onPause = { if (state.isPaused) engine.resume() else engine.pause() },
                    onCancel = { isSettingsOpen = true }
                )
            }
        }
    }

    LaunchedEffect(state.isPaused, state.isFinished, isSettingsOpen) {
        while (!state.isPaused && !state.isFinished && !isSettingsOpen) {
            kotlinx.coroutines.delay(1000)
            engine.updateTick(
                delta = 1.seconds,
                distanceDelta = 2.0,
                stepsDelta = 2,
                heartRate = (130..155).random()
            )
        }
    }
}

@Composable
fun WearSettingsScreen(
    settings: RaceSettings,
    onSettingsChange: (RaceSettings) -> Unit,
    onProfileCycle: () -> Unit,
    onStart: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        item { Text(text = "RACE PROFILES", style = MaterialTheme.typography.caption1, color = Color.Green) }

        // Profile Selector (Cycles between the 3 types)
        item {
            Chip(
                onClick = onProfileCycle,
                label = { Text(text = settings.name) },
                secondaryLabel = { Text(text = "Tap to switch type") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color.DarkGray)
            )
        }

        item { 
            Text(
                text = "Edit ${settings.name} below:", 
                style = MaterialTheme.typography.caption2, 
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp)
            ) 
        }

        // Distance
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(text = "Distance: ${settings.distanceValue.toInt()} mi", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.distanceValue.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(distanceValue = it.toDouble())) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Goal
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(text = "Goal: ${settings.goalTime.inWholeMinutes} min", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.goalTime.inWholeMinutes.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(goalTime = it.toInt().minutes)) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 5f..60f,
                    steps = 54,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Walk
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(text = "Walk: ${settings.walkDuration.inWholeMinutes} min", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.walkDuration.inWholeMinutes.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(walkDuration = it.toInt().minutes)) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Run
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(text = "Run: ${settings.runDuration.inWholeMinutes} min", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.runDuration.inWholeMinutes.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(runDuration = it.toInt().minutes)) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Target HR (Run)
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(text = "Target HR (Run): ${settings.targetHeartRateRun} bpm", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.targetHeartRateRun.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(targetHeartRateRun = it.toInt())) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 100f..200f,
                    steps = 20,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text(text = "START", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WearRaceScreen(
    state: RaceState,
    settings: RaceSettings,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)
    ) {
        item {
            Text(
                text = if (state.currentType == RaceType.WALK) "WALK" else "RUN",
                style = MaterialTheme.typography.title1,
                color = if (state.currentType == RaceType.WALK) Color.Green else Color.Red
            )
        }
        
        item {
            Text(
                text = formatDuration(state.sequenceRemainingTime),
                style = MaterialTheme.typography.display1
            )
        }

        item {
            Text(
                text = "${"%.2f".format(state.totalDistance / 1609.34)} / ${settings.distanceValue.toInt()} mi",
                style = MaterialTheme.typography.body2
            )
        }

        if (state.heartRateAlert) {
            item {
                Text(
                    text = "⚠️ HR HIGH: ${state.currentHeartRate}",
                    style = MaterialTheme.typography.caption1,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Text(
                text = state.isOnTrack(settings),
                style = MaterialTheme.typography.caption2,
                color = when (state.isOnTrack(settings)) {
                    "TOO SLOW" -> Color.Red
                    "TOO FAST" -> Color.Yellow
                    else -> Color.Cyan
                }
            )
        }

        item {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onPause, modifier = Modifier.size(ButtonDefaults.SmallButtonSize)) {
                    Text(text = if (state.isPaused) "►" else "||")
                }
                Button(
                    onClick = onCancel, 
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
                ) {
                    Text(text = "X")
                }
            }
        }
    }
}

package com.example.runapp

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

@Composable
fun WearApp() {
    var settings by remember { mutableStateOf(RaceSettings()) }
    var state by remember { mutableStateOf(RaceState()) }
    var isSettingsOpen by remember { mutableStateOf(true) }

    val engine = remember {
        RaceEngine(
            settings = settings,
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
                    settings = settings,
                    onSettingsChange = { settings = it },
                    onStart = {
                        engine.updateSettings(settings)
                        engine.start()
                        isSettingsOpen = false
                    }
                )
            } else {
                WearRaceScreen(
                    state = state,
                    settings = settings,
                    onPause = { if (state.isPaused) engine.resume() else engine.pause() },
                    onCancel = { isSettingsOpen = true }
                )
            }
        }
    }
}

@Composable
fun WearSettingsScreen(
    settings: RaceSettings,
    onSettingsChange: (RaceSettings) -> Unit,
    onStart: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        item { Text(text = "RACE SETTINGS", style = MaterialTheme.typography.caption1, color = Color.Green) }

        // Race Name (Static for now in Watch UI as typing is hard)
        item {
            Chip(
                onClick = { },
                label = { Text(text = settings.name) },
                secondaryLabel = { Text(text = "Race Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Distance
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Distance: ${settings.distanceValue} ${settings.distanceUnit}", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.distanceValue.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(distanceValue = it.toDouble())) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 0.5f..10.0f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Goal Time
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Goal: ${settings.goalTime.inWholeMinutes} min", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.goalTime.inWholeMinutes.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(goalTime = it.toInt().minutes)) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 5f..120f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Walk Time
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Walk: ${settings.walkDuration.inWholeMinutes} min", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.walkDuration.inWholeMinutes.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(walkDuration = it.toInt().minutes)) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 1f..10f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Run Time
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Run: ${settings.runDuration.inWholeMinutes} min", style = MaterialTheme.typography.body2)
                InlineSlider(
                    value = settings.runDuration.inWholeMinutes.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(runDuration = it.toInt().minutes)) },
                    increaseIcon = { Text(text = "+") },
                    decreaseIcon = { Text(text = "-") },
                    valueRange = 1f..10f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Heart Rate Target (Run)
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Target HR (Run): ${settings.targetHeartRateRun}", style = MaterialTheme.typography.body2)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
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
                text = "${"%.2f".format(state.totalDistance / 1609.34)} / ${settings.distanceValue} mi",
                style = MaterialTheme.typography.body2
            )
        }

        if (state.heartRateAlert) {
            item {
                Text(
                    text = "⚠️ HR TOO HIGH: ${state.currentHeartRate}",
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

package com.example.runapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WearApp() {
    val context = LocalContext.current
    var profiles by remember { mutableStateOf(DefaultProfiles) }
    var selectedProfileIndex by remember { mutableStateOf(0) }
    val currentSettings = profiles[selectedProfileIndex]
    
    var state by remember { mutableStateOf(RaceState()) }
    var isSettingsOpen by remember { mutableStateOf(true) }
    var isRecapOpen by remember { mutableStateOf(false) }
    var isAlertDismissed by remember { mutableStateOf(false) }
    var raceTimestamp by remember { mutableStateOf("") }

    val engine = remember {
        RaceEngine(
            settings = currentSettings,
            onStateUpdate = { 
                state = it 
                if (!it.heartRateAlert) isAlertDismissed = false
                if (it.isFinished && !isSettingsOpen && !isRecapOpen) {
                    raceTimestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
                    isRecapOpen = true
                }
            },
            onSequenceChange = { _, _ -> }
        )
    }

    MaterialTheme {
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = rememberScalingLazyListState()) }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isSettingsOpen) {
                    WearSettingsScreen(
                        settings = currentSettings,
                        onSettingsChange = { updated ->
                            val newList = profiles.toMutableList()
                            newList[selectedProfileIndex] = updated
                            profiles = newList
                        },
                        onProfileCycle = {
                            selectedProfileIndex = (selectedProfileIndex + 1) % profiles.size
                        },
                        onStart = {
                            engine.updateSettings(profiles[selectedProfileIndex])
                            engine.start()
                            isSettingsOpen = false
                            isRecapOpen = false
                        }
                    )
                } else if (isRecapOpen) {
                    WearRecapScreen(
                        state = state,
                        settings = currentSettings,
                        timestamp = raceTimestamp,
                        onClose = { 
                            isSettingsOpen = true
                            isRecapOpen = false
                        }
                    )
                } else {
                    WearRaceScreen(
                        state = state,
                        settings = currentSettings,
                        onPause = { if (state.isPaused) engine.resume() else engine.pause() },
                        onCancel = { isSettingsOpen = true }
                    )

                    if (state.heartRateAlert && !isAlertDismissed) {
                        HeartRateAlertOverlay(
                            currentHeartRate = state.currentHeartRate,
                            onDismiss = { isAlertDismissed = true }
                        )
                    }
                }
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
                heartRate = (110..165).random()
            )
        }
    }
}

@Composable
fun WearRecapScreen(state: RaceState, settings: RaceSettings, timestamp: String, onClose: () -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        item { Text(text = "RACE RECAP", style = MaterialTheme.typography.title2, color = Color.Cyan) }
        item { Text(text = settings.name, style = MaterialTheme.typography.caption1) }
        item { Text(text = timestamp, style = MaterialTheme.typography.caption2, color = Color.Gray) }
        
        item { Spacer(Modifier.height(8.dp)) }
        
        item { RecapItem("Distance", "${"%.2f".format(state.totalDistance / 1609.34)} mi") }
        item { RecapItem("Total Time", formatDuration(state.totalTime)) }
        item { RecapItem("Avg Pace", state.currentPace + " /mi") }
        item { RecapItem("Total Steps", "${state.totalSteps}") }
        item { RecapItem("Min/Max HR", "${if(state.minHeartRate == Int.MAX_VALUE) 0 else state.minHeartRate}/${state.maxHeartRate}") }
        
        item {
            Button(
                onClick = onClose,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
            ) {
                Text("DONE")
            }
        }
    }
}

@Composable
fun RecapItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.caption2, color = Color.LightGray)
        Text(text = value, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HeartRateAlertOverlay(currentHeartRate: Int, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.9f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⚠️ HR HIGH", style = MaterialTheme.typography.title1, color = Color.White)
            Text(text = "$currentHeartRate", fontSize = 48.sp, color = Color.Yellow, fontWeight = FontWeight.ExtraBold)
            Text(text = "BPM", style = MaterialTheme.typography.caption1, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
                Text(text = "OK", color = Color.Red)
            }
        }
    }
}

@Composable
fun WearSettingsScreen(settings: RaceSettings, onSettingsChange: (RaceSettings) -> Unit, onProfileCycle: () -> Unit, onStart: () -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        item { Text(text = "RACE PROFILES", style = MaterialTheme.typography.caption1, color = Color.Green) }
        item {
            Chip(
                onClick = onProfileCycle,
                label = { Text(text = settings.name) },
                secondaryLabel = { Text(text = "Tap to switch") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
        }
        item { Text(text = "Settings:", style = MaterialTheme.typography.caption2, color = Color.Gray) }
        item {
            SettingSlider("Dist", "${settings.distanceValue.toInt()} mi", settings.distanceValue.toFloat(), 1f..10f, 8) {
                onSettingsChange(settings.copy(distanceValue = it.toDouble()))
            }
        }
        item {
            SettingSlider("Walk", "${settings.walkDuration.inWholeMinutes} min", settings.walkDuration.inWholeMinutes.toFloat(), 1f..10f, 8) {
                onSettingsChange(settings.copy(walkDuration = it.toInt().minutes))
            }
        }
        item {
            SettingSlider("Run", "${settings.runDuration.inWholeMinutes} min", settings.runDuration.inWholeMinutes.toFloat(), 1f..10f, 8) {
                onSettingsChange(settings.copy(runDuration = it.toInt().minutes))
            }
        }
        item {
            SettingSlider("HR Walk", "${settings.targetHeartRateWalk} bpm", settings.targetHeartRateWalk.toFloat(), 80f..150f, 7) {
                onSettingsChange(settings.copy(targetHeartRateWalk = it.toInt()))
            }
        }
        item {
            SettingSlider("HR Run", "${settings.targetHeartRateRun} bpm", settings.targetHeartRateRun.toFloat(), 100f..200f, 10) {
                onSettingsChange(settings.copy(targetHeartRateRun = it.toInt()))
            }
        }
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))) {
                Text(text = "START", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingSlider(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValueChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(text = "$label: $valueText", style = MaterialTheme.typography.body2)
        InlineSlider(
            value = value,
            onValueChange = onValueChange,
            increaseIcon = { Text("+") },
            decreaseIcon = { Text("-") },
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WearRaceScreen(state: RaceState, settings: RaceSettings, onPause: () -> Unit, onCancel: () -> Unit) {
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
        item { Text(text = formatDuration(state.sequenceRemainingTime), style = MaterialTheme.typography.display1) }
        item { Text(text = "${"%.2f".format(state.totalDistance / 1609.34)} / ${settings.distanceValue.toInt()} mi", style = MaterialTheme.typography.body2) }
        item { Text(text = "HR: ${state.currentHeartRate} bpm", style = MaterialTheme.typography.caption2, color = if (state.heartRateAlert) Color.Red else Color.Gray) }
        item { Text(text = state.isOnTrack(settings), style = MaterialTheme.typography.caption2, color = if (state.isOnTrack(settings) == "TOO SLOW") Color.Red else Color.Cyan) }
        item {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPause, modifier = Modifier.size(ButtonDefaults.SmallButtonSize)) { Text(if (state.isPaused) "►" else "||") }
                Button(onClick = onCancel, modifier = Modifier.size(ButtonDefaults.SmallButtonSize), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) { Text("X") }
            }
        }
    }
}

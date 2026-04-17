package com.example.runapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.seconds

@Composable
fun App(
    onEngineCreated: (RaceEngine) -> Unit = {}
) {
    var settings by remember { mutableStateOf(RaceSettings()) }
    var state by remember { mutableStateOf(RaceState()) }
    var isSettingsOpen by remember { mutableStateOf(true) }

    val engine = remember {
        RaceEngine(
            settings = settings,
            onStateUpdate = { state = it },
            onSequenceChange = { type, duration ->
                // Visual/Haptic feedback trigger
            }
        ).also { onEngineCreated(it) }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(contentAlignment = Alignment.Center) {
                if (isSettingsOpen) {
                    SettingsScreen(
                        settings = settings,
                        onSettingsChange = { settings = it },
                        onStart = {
                            engine.updateSettings(settings)
                            engine.start()
                            isSettingsOpen = false
                        }
                    )
                } else {
                    RaceScreen(
                        state = state,
                        settings = settings,
                        onPause = { if (state.isPaused) engine.resume() else engine.pause() },
                        onCancel = { isSettingsOpen = true }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: RaceSettings,
    onSettingsChange: (RaceSettings) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "RUN APP", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(text = "Dist: ${settings.distanceValue} mi", color = Color.LightGray)
        Slider(
            value = settings.distanceValue.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(distanceValue = it.toDouble())) },
            valueRange = 0.5f..5.0f,
            modifier = Modifier.height(30.dp)
        )
        Text(text = "Goal: ${settings.goalTime.inWholeMinutes}m", color = Color.LightGray)
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
        ) {
            Text(text = "GO!", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RaceScreen(
    state: RaceState,
    settings: RaceSettings,
    onPause: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (state.currentType == RaceType.WALK) "WALK" else "RUN",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (state.currentType == RaceType.WALK) Color.Green else Color.Red,
            textAlign = TextAlign.Center
        )
        Text(
            text = formatDuration(state.sequenceRemainingTime),
            fontSize = 40.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${"%.2f".format(state.totalDistance / 1609.34)} / ${settings.distanceValue} mi",
            fontSize = 14.sp,
            color = Color.LightGray
        )
        Text(
            text = state.isOnTrack(settings),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = when (state.isOnTrack(settings)) {
                "TOO SLOW" -> Color.Red
                "TOO FAST" -> Color.Yellow
                else -> Color.Blue
            }
        )
        
        Text(text = "HR: ${state.currentHeartRate} bpm", fontSize = 10.sp, color = Color.Gray)
        
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPause, modifier = Modifier.weight(1f).height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) {
                Text(text = if (state.isPaused) "►" else "||", color = Color.White)
            }
            Button(onClick = onCancel, modifier = Modifier.weight(1f).height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFB71C1C))) {
                Text(text = "X", color = Color.White)
            }
        }
        if (state.isFinished) {
            Text(text = "DONE!", fontSize = 24.sp, color = Color.Cyan, fontWeight = FontWeight.ExtraBold)
        }
    }
}

package com.example.runapp

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun WearApp(
    onStartRace: () -> Unit = {},
    onFinishRace: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("race_prefs", Context.MODE_PRIVATE) }
    
    var profiles by remember { mutableStateOf(loadProfiles(prefs)) }
    var selectedProfileIndex by remember { mutableStateOf(0) }
    val currentSettings = profiles[selectedProfileIndex]
    
    var state by remember { mutableStateOf(RaceState()) }
    var isSettingsOpen by remember { mutableStateOf(true) }
    var isRecapOpen by remember { mutableStateOf(false) }
    var isAlertDismissed by remember { mutableStateOf(false) }
    var raceTimestamp by remember { mutableStateOf("") }

    var liveHeartRate by remember { mutableIntStateOf(0) }
    var liveSteps by remember { mutableIntStateOf(0) }
    var lastSteps by remember { mutableIntStateOf(-1) }
    var distanceMoved by remember { mutableDoubleStateOf(0.0) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }

    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    val engine = remember {
        RaceEngine(
            settings = currentSettings,
            onStateUpdate = { 
                state = it 
                if (!it.heartRateAlert) isAlertDismissed = false
                if (it.isFinished && !isSettingsOpen && !isRecapOpen) {
                    raceTimestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
                    isRecapOpen = true
                    onFinishRace()
                    // Save for Tile
                    prefs.edit().putString("last_race_summary", "${"%.2f".format(it.totalDistance / 1609.34)} mi in ${formatDuration(it.totalTime)}").apply()
                }
            },
            onSequenceChange = { type, _ ->
                val pattern = if (type == RaceType.RUN) longArrayOf(0, 500, 200, 500) else longArrayOf(0, 200, 100, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        )
    }

    DisposableEffect(isSettingsOpen) {
        if (isSettingsOpen) return@DisposableEffect onDispose {}

        val hrListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_HEART_RATE) liveHeartRate = event.values[0].toInt()
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        val stepListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val totalSteps = event.values[0].toInt()
                    if (lastSteps == -1) lastSteps = totalSteps
                    liveSteps = totalSteps - lastSteps
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        val locListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lastLocation?.let { distanceMoved += it.distanceTo(location).toDouble() }
                lastLocation = location
            }
        }
        sensorManager.registerListener(hrListener, sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE), SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(stepListener, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_UI)
        try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locListener) } catch (e: SecurityException) {}

        onDispose {
            sensorManager.unregisterListener(hrListener)
            sensorManager.unregisterListener(stepListener)
            locationManager.removeUpdates(locListener)
        }
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
                            saveProfiles(prefs, newList)
                        },
                        onProfileCycle = { selectedProfileIndex = (selectedProfileIndex + 1) % profiles.size },
                        onStart = {
                            distanceMoved = 0.0
                            lastSteps = -1
                            liveSteps = 0
                            lastLocation = null
                            engine.updateSettings(profiles[selectedProfileIndex])
                            engine.start()
                            isSettingsOpen = false
                            isRecapOpen = false
                            onStartRace()
                        }
                    )
                } else if (isRecapOpen) {
                    WearRecapScreen(state = state, settings = currentSettings, timestamp = raceTimestamp, onClose = { isSettingsOpen = true; isRecapOpen = false })
                } else {
                    WearRaceScreen(state = state, settings = currentSettings, onPause = { if (state.isPaused) engine.resume() else engine.pause() }, onCancel = { isSettingsOpen = true; onFinishRace() })
                    if (state.heartRateAlert && !isAlertDismissed) {
                        HeartRateAlertOverlay(currentHeartRate = state.currentHeartRate, onDismiss = { isAlertDismissed = true })
                    }
                }
            }
        }
    }

    LaunchedEffect(state.isPaused, state.isFinished, isSettingsOpen) {
        var lastDistance = 0.0
        var lastStepCount = 0
        while (!state.isPaused && !state.isFinished && !isSettingsOpen) {
            kotlinx.coroutines.delay(1000)
            val dDist = distanceMoved - lastDistance
            val dSteps = liveSteps - lastStepCount
            engine.updateTick(delta = 1.seconds, distanceDelta = dDist, stepsDelta = dSteps, heartRate = liveHeartRate)
            lastDistance = distanceMoved
            lastStepCount = liveSteps
        }
    }
}

fun saveProfiles(prefs: SharedPreferences, profiles: List<RaceSettings>) {
    val editor = prefs.edit()
    profiles.forEach { profile ->
        val prefix = "profile_${profile.id}_"
        editor.putFloat("${prefix}dist", profile.distanceValue.toFloat())
        editor.putLong("${prefix}goal", profile.goalTime.inWholeMinutes)
        editor.putLong("${prefix}walk", profile.walkDuration.inWholeMinutes)
        editor.putLong("${prefix}run", profile.runDuration.inWholeMinutes)
        editor.putInt("${prefix}hr_walk", profile.targetHeartRateWalk)
        editor.putInt("${prefix}hr_run", profile.targetHeartRateRun)
    }
    editor.apply()
}

fun loadProfiles(prefs: SharedPreferences): List<RaceSettings> {
    return DefaultProfiles.map { def ->
        val prefix = "profile_${def.id}_"
        if (!prefs.contains("${prefix}dist")) return@map def
        def.copy(
            distanceValue = prefs.getFloat("${prefix}dist", def.distanceValue.toFloat()).toDouble(),
            goalTime = prefs.getLong("${prefix}goal", def.goalTime.inWholeMinutes).minutes,
            walkDuration = prefs.getLong("${prefix}walk", def.walkDuration.inWholeMinutes).minutes,
            runDuration = prefs.getLong("${prefix}run", def.runDuration.inWholeMinutes).minutes,
            targetHeartRateWalk = prefs.getInt("${prefix}hr_walk", def.targetHeartRateWalk),
            targetHeartRateRun = prefs.getInt("${prefix}hr_run", def.targetHeartRateRun)
        )
    }
}

@Composable
fun WearRecapScreen(state: RaceState, settings: RaceSettings, timestamp: String, onClose: () -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)) {
        item { Text(text = "RACE RECAP", style = MaterialTheme.typography.title2, color = Color.Cyan) }
        item { Text(text = timestamp, style = MaterialTheme.typography.caption2, color = Color.Gray) }
        item { Spacer(Modifier.height(8.dp)) }
        item { RecapItem("Total Time", formatDuration(state.totalTime)) }
        item { RecapItem("Distance", "${"%.2f".format(state.totalDistance / 1609.34)} mi") }
        item { RecapItem("Avg Pace", state.currentPace + " /mi") }
        item { RecapItem("Total Steps", "${state.totalSteps}") }
        item { RecapItem("Min/Max HR", "${if(state.minHeartRate == Int.MAX_VALUE) 0 else state.minHeartRate}/${state.maxHeartRate}") }
        item { Button(onClick = onClose, modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) { Text("DONE") } }
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
    Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.9f)).padding(20.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⚠️ HR HIGH", style = MaterialTheme.typography.title1, color = Color.White)
            Text(text = "$currentHeartRate", fontSize = 48.sp, color = Color.Yellow, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) { Text(text = "OK", color = Color.Red) }
        }
    }
}

@Composable
fun WearSettingsScreen(settings: RaceSettings, onSettingsChange: (RaceSettings) -> Unit, onProfileCycle: () -> Unit, onStart: () -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)) {
        item { Text(text = "RACE PROFILES", style = MaterialTheme.typography.caption1, color = Color.Green) }
        item { Chip(onClick = onProfileCycle, label = { Text(text = settings.name) }, secondaryLabel = { Text(text = "Tap to switch") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) }
        item { Text(text = "Settings:", style = MaterialTheme.typography.caption2, color = Color.Gray) }
        item { SettingSlider("Dist", "${settings.distanceValue.toInt()} mi", settings.distanceValue.toFloat(), 1f..10f, 8) { onSettingsChange(settings.copy(distanceValue = it.toDouble())) } }
        item { SettingSlider("Goal", "${settings.goalTime.inWholeMinutes} min", settings.goalTime.inWholeMinutes.toFloat(), 5f..60f, 55) { onSettingsChange(settings.copy(goalTime = it.toInt().minutes)) } }
        item { SettingSlider("Walk", "${settings.walkDuration.inWholeMinutes} min", settings.walkDuration.inWholeMinutes.toFloat(), 1f..10f, 8) { onSettingsChange(settings.copy(walkDuration = it.toInt().minutes)) } }
        item { SettingSlider("Run", "${settings.runDuration.inWholeMinutes} min", settings.runDuration.inWholeMinutes.toFloat(), 1f..10f, 8) { onSettingsChange(settings.copy(runDuration = it.toInt().minutes)) } }
        item { SettingSlider("HR Walk", "${settings.targetHeartRateWalk} bpm", settings.targetHeartRateWalk.toFloat(), 80f..150f, 7) { onSettingsChange(settings.copy(targetHeartRateWalk = it.toInt())) } }
        item { SettingSlider("HR Run", "${settings.targetHeartRateRun} bpm", settings.targetHeartRateRun.toFloat(), 100f..200f, 10) { onSettingsChange(settings.copy(targetHeartRateRun = it.toInt())) } }
        item { Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))) { Text(text = "START", fontWeight = FontWeight.Bold) } }
    }
}

@Composable
fun SettingSlider(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValueChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(text = "$label: $valueText", style = MaterialTheme.typography.body2)
        InlineSlider(value = value, onValueChange = onValueChange, increaseIcon = { Text("+") }, decreaseIcon = { Text("-") }, valueRange = range, steps = steps, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun WearRaceScreen(state: RaceState, settings: RaceSettings, onPause: () -> Unit, onCancel: () -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp)) {
        item { Text(text = if (state.currentType == RaceType.WALK) "WALK" else "RUN", style = MaterialTheme.typography.title1, color = if (state.currentType == RaceType.WALK) Color.Green else Color.Red) }
        item { Text(text = formatDuration(state.sequenceRemainingTime), style = MaterialTheme.typography.display1) }
        item { Text(text = "${"%.2f".format(state.totalDistance / 1609.34)} / ${settings.distanceValue.toInt()} mi", style = MaterialTheme.typography.body2) }
        item { Text(text = "HR: ${state.currentHeartRate} bpm", style = MaterialTheme.typography.caption2, color = if (state.heartRateAlert) Color.Red else Color.Gray) }
        item { Text(text = state.isOnTrack(settings), style = MaterialTheme.typography.caption2, color = if (state.isOnTrack(settings) == "TOO SLOW") Color.Red else Color.Cyan) }
        item { Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = onPause, modifier = Modifier.size(ButtonDefaults.SmallButtonSize)) { Text(if (state.isPaused) "►" else "||") }
        Button(onClick = onCancel, modifier = Modifier.size(ButtonDefaults.SmallButtonSize), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) { Text("X") } } }
    }
}

package com.example.runapp

import kotlin.time.Duration

fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val positiveSeconds = if (totalSeconds < 0) 0 else totalSeconds
    val mins = positiveSeconds / 60
    val secs = positiveSeconds % 60
    return "%02d:%02d".format(mins, secs)
}

package com.example.runapp

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

class RaceService : Service() {
    private val binder = LocalBinder()
    private val notificationId = 101
    private val channelId = "race_channel"

    inner class LocalBinder : Binder() {
        fun getService(): RaceService = this@RaceService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun startForegroundService() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Race Active")
            .setContentText("Tracking your 5K...")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)

        val ongoingActivityStatus = Status.Builder()
            .addPart("race_status", Status.TextPart("Race in progress"))
            .build()

        val ongoingActivity = OngoingActivity.Builder(this, notificationId, notificationBuilder)
            .setStaticIcon(android.R.drawable.ic_media_play)
            .setTouchIntent(pendingIntent)
            .setStatus(ongoingActivityStatus)
            .build()

        ongoingActivity.apply(this)
        startForeground(notificationId, notificationBuilder.build())
    }

    fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(channelId, "Race Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

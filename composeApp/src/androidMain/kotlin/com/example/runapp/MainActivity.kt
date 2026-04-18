package com.example.runapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.wear.ambient.AmbientLifecycleObserver

class MainActivity : ComponentActivity() {
    private var raceService: RaceService? = null
    private var isBound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RaceService.LocalBinder
            raceService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            // App entered Always-on/Ambient mode
        }
        override fun onExitAmbient() {
            // App woke up
        }
    }

    private val ambientObserver = AmbientLifecycleObserver(this, ambientCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycle.addObserver(ambientObserver)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        setContent {
            val isWear = packageName.endsWith(".wear")
            if (isWear) {
                WearApp(
                    onStartRace = { 
                        startService(Intent(this, RaceService::class.java))
                        bindService(Intent(this, RaceService::class.java), connection, Context.BIND_AUTO_CREATE)
                        raceService?.startForegroundService()
                    },
                    onFinishRace = {
                        raceService?.stopForegroundService()
                        if (isBound) unbindService(connection)
                        isBound = false
                    }
                )
            } else {
                App()
            }
        }
    }
}

package com.example.runapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Using a safer check that doesn't rely on BuildConfig if it's failing
            // We can determine the flavor by the package name if needed
            val isWear = packageName.endsWith(".wear")
            
            if (isWear) {
                WearApp()
            } else {
                App()
            }
        }
    }
}

package com.example.skbt_up_gibdd_eyewitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.skbt_up_gibdd_eyewitness.app.EyewitnessApp
import com.example.skbt_up_gibdd_eyewitness.ui.theme.SKBTUPGIBDDEYEWITNESSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SKBTUPGIBDDEYEWITNESSTheme {
                EyewitnessApp()
            }
        }
    }
}

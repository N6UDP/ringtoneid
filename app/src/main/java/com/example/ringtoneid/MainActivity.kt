package com.example.ringtoneid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ringtoneid.ui.navigation.RingtoneIdNavGraph
import com.example.ringtoneid.ui.theme.RingtoneIDTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RingtoneIDTheme {
                RingtoneIdNavGraph()
            }
        }
    }
}

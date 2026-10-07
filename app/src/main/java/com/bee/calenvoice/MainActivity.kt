package com.bee.calenvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bee.calenvoice.ui.VoiceCaptureScreen

import com.bee.calenvoice.ui.theme.CalenVoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalenVoiceTheme {
                // UI dulu — belum ada logic. Ganti dengan viewModel.uiState dan
                // callback VoiceReminderViewModel setelah repository terisi.
                VoiceCaptureScreen(onRecord = {})
            }
        }
    }
}

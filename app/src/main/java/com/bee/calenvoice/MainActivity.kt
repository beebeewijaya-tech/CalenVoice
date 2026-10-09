package com.bee.calenvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bee.calenvoice.ui.DemoNavHost
import com.bee.calenvoice.ui.theme.CalenVoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalenVoiceTheme {
                // SEMENTARA — navigasi dummy untuk menguji ketiga tampilan.
                // Ganti dengan VoiceReminderViewModel setelah repository terisi,
                // lalu hapus ui/DemoNavHost.kt.
                DemoNavHost()
            }
        }
    }
}

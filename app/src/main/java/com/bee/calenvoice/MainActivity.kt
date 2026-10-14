package com.bee.calenvoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bee.calenvoice.data.AndroidSpeechRepository
import com.bee.calenvoice.data.DeviceCalendarRepository
import com.bee.calenvoice.data.GeminiInferenceRepository
import com.bee.calenvoice.route.VoiceReminderRoute
import com.bee.calenvoice.ui.VoiceReminderViewModel
import com.bee.calenvoice.ui.theme.CalenVoiceTheme

/**
 * Titik masuk aplikasi. Di sinilah tiga repository (Lapisan 2) dirakit lalu diberikan
 * ke ViewModel (Lapisan 1). AndroidSpeechRepository dan DeviceCalendarRepository sama-sama
 * memakai applicationContext supaya tidak menahan Activity di memori.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: VoiceReminderViewModel by viewModels {
        viewModelFactory {
            initializer {
                VoiceReminderViewModel(
                    speech = AndroidSpeechRepository(applicationContext),
                    inference = GeminiInferenceRepository(),
                    calendar = DeviceCalendarRepository(applicationContext),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalenVoiceTheme {
                VoiceReminderRoute(viewModel)
            }
        }
    }
}
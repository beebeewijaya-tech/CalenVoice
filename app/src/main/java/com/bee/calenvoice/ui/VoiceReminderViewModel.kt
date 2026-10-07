package com.bee.calenvoice.ui

import androidx.lifecycle.ViewModel
import com.bee.calenvoice.data.CalendarRepository
import com.bee.calenvoice.data.InferenceRepository
import com.bee.calenvoice.data.SpeechRepository
import com.bee.calenvoice.model.RecordingState
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** State yang turun dari ViewModel ke View sebagai StateFlow (MVVM, Gambar 2.2). */
data class VoiceReminderUiState(
    val recording: RecordingState = RecordingState.IDLE,
    val elapsedSeconds: Int = 0,
    val transcriptText: String = "",
    val reminder: Reminder? = null,
    val savedEventId: Long? = null,
    val error: String? = null,
) {
    companion object {
        const val MAX_SECONDS = 60 // FR-03
    }
}

/**
 * Lapisan 1 — pemegang urutan langkah dan seluruh keputusan alur (Tabel 2.2).
 * Murni Kotlin: tidak menyentuh API Android langsung.
 */
class VoiceReminderViewModel(
    private val speech: SpeechRepository,
    private val inference: InferenceRepository,
    private val calendar: CalendarRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceReminderUiState())
    val uiState: StateFlow<VoiceReminderUiState> = _uiState.asStateFlow()

    /** Fitur 1 → validasi regex → Fitur 2 (Gambar 4.1 dan 4.2). */
    fun onRecordClicked() { TODO() }

    /** Berhenti lebih awal; auto-stop di detik 60 juga lewat sini. */
    fun onStopClicked() { TODO() }

    /** Batal — kembali ke IDLE tanpa memanggil Gemini. */
    fun onCancelClicked() { TODO() }

    /** Fitur 3 — simpan draf yang sedang tampil ke kalender perangkat (Gambar 4.3). */
    fun onSaveClicked() { TODO() }

    fun onRetry() { TODO() }
}

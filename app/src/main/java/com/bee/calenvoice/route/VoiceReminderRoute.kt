package com.bee.calenvoice.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bee.calenvoice.ui.VoiceCaptureScreen
import com.bee.calenvoice.ui.VoiceReminderViewModel

/**
 * Pemilih layar dalam pola MVVM. Tidak punya logika alur sendiri: keputusan
 * "layar mana yang tampil" hanya dibaca dari uiState milik ViewModel.
 *
 * Selama belum ada Reminder hasil Gemini, tampilkan layar rekam (Fitur 1 dan 2).
 * Begitu reminder terisi, pindah ke layar hasil (Fitur 3). Setelah pengguna menekan
 * "Selesai", ViewModel mereset state sehingga reminder kembali null dan layar rekam
 * muncul lagi untuk reminder berikutnya.
 *
 * ReminderResultRoute berada di package yang sama (route), jadi tidak perlu di-import.
 * Kalau tim nanti memakai Navigation Compose, aturan yang sama bisa dipindahkan ke NavHost.
 */
@Composable
fun VoiceReminderRoute(
    viewModel: VoiceReminderViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    if (state.reminder != null) {
        ReminderResultRoute(viewModel, modifier)
    } else {
        VoiceCaptureScreen(
            state = state,
            onRecord = viewModel::onRecordClicked,
            onCancel = viewModel::onCancelClicked,
            modifier = modifier,
        )
    }
}
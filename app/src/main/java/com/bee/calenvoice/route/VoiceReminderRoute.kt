package com.bee.calenvoice.route

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * Route juga menjalankan hal yang hanya bisa dilakukan lapisan UI, yaitu menampilkan
 * dialog izin mikrofon dan membuka layar Pengaturan. SpeechRepository sengaja tidak
 * menyentuh UI, sama seperti CalendarRepository pada Fitur 3.
 *
 * Alur izin: ViewModel mengangkat flag needsMicPermission, Route meluncurkan dialog,
 * hasilnya dikembalikan ke ViewModel lewat onMicPermissionResult.
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
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onMicPermissionResult(granted)
    }

    LaunchedEffect(state.needsMicPermission) {
        if (state.needsMicPermission) {
            // Reset flag dulu supaya dialog tidak terpicu ulang saat recomposition.
            viewModel.onMicPermissionRequestHandled()
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (state.reminder != null) {
        ReminderResultRoute(viewModel, modifier)
    } else {
        VoiceCaptureScreen(
            state = state,
            onRecord = viewModel::onRecordClicked,
            onStop = viewModel::onStopClicked,
            onCancel = viewModel::onCancelClicked,
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            },
            modifier = modifier,
        )
    }
}

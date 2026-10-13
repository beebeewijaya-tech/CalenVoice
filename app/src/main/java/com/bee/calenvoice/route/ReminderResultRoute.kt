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
import com.bee.calenvoice.ui.ReminderResultScreen
import com.bee.calenvoice.ui.VoiceReminderViewModel

/**
 * Penghubung Fitur 3 dalam pola MVVM. Berada di package `route` (terpisah dari `ui`),
 * sehingga layar dan ViewModel dari package `ui` harus di-import secara eksplisit.
 *
 * Tugasnya:
 * 1. Mengamati uiState dari ViewModel (state turun).
 * 2. Meneruskan klik tombol ke ViewModel (event naik).
 * 3. Menjalankan hal yang hanya bisa dilakukan lapisan UI, yaitu menampilkan dialog izin
 *    kalender dan membuka layar Pengaturan aplikasi (pengganti requestCalendarPermission()
 *    di Gambar 3.3, karena repository tidak boleh menyentuh UI).
 *
 * Alur izin: ViewModel mengangkat flag needsCalendarPermission, Route meluncurkan dialog,
 * hasilnya dikembalikan ke ViewModel lewat onCalendarPermissionResult.
 */
@Composable
fun ReminderResultRoute(
    viewModel: VoiceReminderViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_CALENDAR] == true &&
                result[Manifest.permission.WRITE_CALENDAR] == true
        viewModel.onCalendarPermissionResult(granted)
    }

    LaunchedEffect(state.needsCalendarPermission) {
        if (state.needsCalendarPermission) {
            // Reset flag dulu supaya dialog tidak terpicu ulang saat recomposition.
            viewModel.onCalendarPermissionRequestHandled()
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        }
    }

    ReminderResultScreen(
        reminder = state.reminder,
        isSaving = state.isSaving,
        errorMessage = state.error,
        isSuccess = state.savedEventId != null,
        isPermissionError = state.errorIsPermission,
        onSave = viewModel::onSaveClicked,
        onRetry = viewModel::onSaveClicked, // retry menyimpan ulang, reminder tetap ada
        onDone = viewModel::onDoneClicked,
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
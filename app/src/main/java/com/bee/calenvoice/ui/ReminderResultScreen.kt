package com.bee.calenvoice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bee.calenvoice.model.Reminder
import com.bee.calenvoice.ui.theme.CalenVoiceTheme

/**
 * Fitur 3: Konfirmasi Draf Reminder dan Simpan ke Kalender HP.
 *
 * Murni View dalam pola MVVM (Gambar 2.2): hanya menggambar state dan meneruskan
 * event ke ViewModel, tanpa logika alur. Tema dan susunan header sengaja disamakan
 * dengan VoiceCaptureScreen supaya dua layar terasa satu aplikasi.
 *
 * Urutan cabang `when` penting: loading menang atas sukses, sukses menang atas error,
 * dan error menang atas draf.
 *
 * @param isPermissionError true jika gagal karena izin kalender, supaya tombol
 * "Buka Pengaturan" ikut tampil (UC-01 Alur Alternatif D).
 * @param onOpenSettings dipanggil saat tombol "Buka Pengaturan" ditekan.
 */
@Composable
fun ReminderResultScreen(
    reminder: Reminder?,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    isSuccess: Boolean = false,
    isPermissionError: Boolean = false,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) = Column(
    modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    // Header aplikasi, sama dengan layar rekam.
    Text(
        "Reminder Suara AI",
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(Modifier.weight(1f))

    // Konten dinamis berdasarkan state penyimpanan kalender.
    when {
        isSaving -> SavingContent()
        isSuccess -> SuccessContent(reminder, onDone)
        errorMessage != null -> ErrorContent(errorMessage, isPermissionError, onRetry, onOpenSettings)
        reminder != null -> DraftContent(reminder, onSave)
    }

    Spacer(Modifier.weight(1f))
}

/** Tampilan loading: saat sistem menulis ke Android CalendarContract. */
@Composable
private fun SavingContent() = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    CircularProgressIndicator(
        Modifier.size(56.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeWidth = 5.dp,
    )

    Spacer(Modifier.height(32.dp))
    Text("Menyimpan...", style = MaterialTheme.typography.titleMedium)

    Spacer(Modifier.height(8.dp))
    Text(
        "Memasukkan agenda ke kalender HP",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Tampilan draf: judul, tanggal, dan jam hasil ekstraksi Gemini (Fitur 2). */
@Composable
private fun DraftContent(
    reminder: Reminder,
    onSave: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth(),
) {
    Text("Agenda Terdeteksi", style = MaterialTheme.typography.titleMedium)

    Spacer(Modifier.height(16.dp))

    // Kartu bergaya sama dengan HintCard di VoiceCaptureScreen.
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            reminder.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                "📅 ${reminder.date}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "⏰ ${reminder.time}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onSave,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text("Simpan ke Kalender")
    }
}

/** Tampilan berhasil: konfirmasi bahwa acara sudah masuk ke kalender perangkat (FR-11). */
@Composable
private fun SuccessContent(
    reminder: Reminder?,
    onDone: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth(),
) {
    Text("✓ Acara Berhasil Disimpan!", style = MaterialTheme.typography.titleMedium)

    if (reminder != null) {
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                "${reminder.date} • ${reminder.time}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text("Selesai")
    }
}

/**
 * Tampilan error: mengakomodasi UC-01 Alur Alternatif D (izin kalender ditolak atau
 * gagal simpan). Tombol "Buka Pengaturan" hanya muncul bila penyebabnya izin, karena
 * "Coba Lagi" saja tidak membantu kalau izin sudah ditolak permanen.
 */
@Composable
private fun ErrorContent(
    message: String,
    showSettingsButton: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth(),
) {
    Text(
        "Gagal Menyimpan",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error,
    )

    Spacer(Modifier.height(16.dp))

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
        )
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text("Coba Lagi")
    }

    if (showSettingsButton) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Buka Pengaturan")
        }
    }
}

@Preview(name = "Draft Hasil Gemini")
@Composable
private fun DraftPreview() = CalenVoiceTheme {
    ReminderResultScreen(
        reminder = Reminder("Meeting Proyek CalenVoice", "2026-10-09", "15:00"),
        onSave = {},
        onRetry = {},
        onDone = {},
    )
}

@Preview(name = "Menyimpan")
@Composable
private fun SavingPreview() = CalenVoiceTheme {
    ReminderResultScreen(
        reminder = Reminder("Meeting Proyek CalenVoice", "2026-10-09", "15:00"),
        isSaving = true,
        onSave = {},
        onRetry = {},
        onDone = {},
    )
}

@Preview(name = "Berhasil Simpan")
@Composable
private fun SuccessPreview() = CalenVoiceTheme {
    ReminderResultScreen(
        reminder = Reminder("Meeting Proyek CalenVoice", "2026-10-09", "15:00"),
        isSuccess = true,
        onSave = {},
        onRetry = {},
        onDone = {},
    )
}

@Preview(name = "Gagal: Izin Kalender")
@Composable
private fun PermissionErrorPreview() = CalenVoiceTheme {
    ReminderResultScreen(
        reminder = Reminder("Meeting Proyek CalenVoice", "2026-10-09", "15:00"),
        errorMessage = "Event tidak bisa disimpan karena izin kalender belum diberikan.",
        isPermissionError = true,
        onSave = {},
        onRetry = {},
        onDone = {},
    )
}
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bee.calenvoice.model.Reminder
import com.bee.calenvoice.ui.theme.CalenVoiceTheme

/**
 * Layar Fitur 3 — Konfirmasi Draf Reminder & Simpan ke Kalender HP
 */
@Composable
fun ReminderResultScreen(
    reminder: Reminder?,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    isSuccess: Boolean = false,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    // Header Aplikasi
    Text(
        "Reminder Suara AI",
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(Modifier.weight(1f))

    // Konten Dinamis Berdasarkan State Penyimpanan Kalender
    when {
        isSaving -> SavingContent()
        isSuccess -> SuccessContent(reminder, onDone)
        errorMessage != null -> ErrorContent(errorMessage, onRetry)
        reminder != null -> DraftContent(reminder, onSave)
    }

    Spacer(Modifier.weight(1f))
}

/** Tampilan Loading — Saat sistem menyimpan ke Android CalendarContract. */
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

/** Tampilan Draf — Menampilkan Judul, Tanggal, dan Jam hasil ekstraksi Gemini AI. */
@Composable
private fun DraftContent(
    reminder: Reminder,
    onSave: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
) {
    Text("Agenda Terdeteksi", style = MaterialTheme.typography.titleMedium)

    Spacer(Modifier.height(16.dp))

    // Menggunakan skema kartu ala MaterialTheme persis seperti HintCard milik Bee
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
            horizontalArrangement = Arrangement.SpaceEvenly
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
            .height(48.dp)
    ) {
        Text("Simpan ke Kalender")
    }
}

/** Tampilan Berhasil — Konfirmasi bahwa acara sudah masuk ke kalender perangkat. */
@Composable
private fun SuccessContent(
    reminder: Reminder?,
    onDone: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
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
                textAlign = TextAlign.Center
            )
            Text(
                "${reminder.date} • ${reminder.time}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("Selesai")
    }
}

/** Tampilan Error — Mengakomodasi Alur Alternatif D (Izin kalender ditolak/gagal). */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth()
) {
    Text("Gagal Menyimpan", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)

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
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("Coba Lagi")
    }
}


@Preview(name = "Draft Hasil Gemini")
@Composable
private fun DraftPreview() = CalenVoiceTheme {
    ReminderResultScreen(
        reminder = Reminder("Meeting Proyek CalenVoice", "2026-10-09", "15:00"),
        onSave = {},
        onRetry = {},
        onDone = {}
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
        onDone = {}
    )
}
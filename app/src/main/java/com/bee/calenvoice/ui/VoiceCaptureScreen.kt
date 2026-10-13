package com.bee.calenvoice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bee.calenvoice.model.RecordingState
import com.bee.calenvoice.ui.theme.CalenVoiceTheme

/**
 * Layar Fitur 1 dan 2: perekaman suara sampai teks diproses Gemini (Gambar L.1).
 *
 * Murni View dalam pola MVVM (Gambar 2.2): hanya menggambar [state] dan meneruskan
 * sentuhan pengguna ke ViewModel lewat callback. Tidak ada logika alur di sini, dan
 * layar ini tidak tahu apa pun soal audio, Gemini, ataupun kalender.
 *
 * Satu layar yang berubah mengikuti [RecordingState]:
 * 1. IDLE: tombol mikrofon dan contoh perintah (sudah ada, lewat [IdleContent]).
 * 2. PROCESSING: loading dan kutipan transkrip (sudah ada, lewat [ProcessingContent]).
 * 3. RECORDING, DONE, dan ERROR: belum digambar. Sementara ini ketiganya jatuh ke
 *    cabang `else` sehingga tampil sama seperti IDLE. Tambahkan sebagai cabang baru
 *    pada `when` di bawah.
 *
 * Perpindahan ke layar hasil (Fitur 3) tidak diurus di sini, melainkan oleh
 * VoiceReminderRoute: begitu `reminder` pada state terisi, layar ini diganti
 * ReminderResultScreen.
 *
 * @param state kondisi layar yang diamati dari StateFlow milik ViewModel.
 * @param onRecord dipanggil saat tombol mikrofon ditekan (onRecordClicked di ViewModel).
 * @param onCancel dipanggil saat tombol "Batal" ditekan (onCancelClicked di ViewModel).
 */
@Composable
fun VoiceCaptureScreen(
    state: VoiceReminderUiState,
    onRecord: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    // Header aplikasi. ReminderResultScreen memakai susunan yang sama supaya
    // dua layar terasa satu aplikasi.
    Text(
        "Reminder Suara AI",
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    // Dua Spacer berbobot sama menaruh konten tepat di tengah layar.
    Spacer(Modifier.weight(1f))

    when (state.recording) {
        RecordingState.PROCESSING -> ProcessingContent(state.transcriptText, onCancel)
        else -> IdleContent(onRecord)
    }

    Spacer(Modifier.weight(1f))
}

/**
 * Tampilan 1: menunggu pengguna menekan mikrofon.
 *
 * Tombol berbentuk lingkaran 160dp dengan deskripsi aksesibilitas "Mulai merekam suara"
 * supaya terbaca oleh pembaca layar (TalkBack). Di bawahnya ada contoh perintah sebagai
 * petunjuk bahwa ucapan harus menyebut jam atau pukul (FR-06).
 */
@Composable
private fun IdleContent(onRecord: () -> Unit) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Surface(
        onClick = onRecord,
        modifier = Modifier
            .size(160.dp)
            .semantics { contentDescription = "Mulai merekam suara" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        MicGlyph(
            MaterialTheme.colorScheme.onPrimary,
            Modifier.padding(52.dp),
        )
    }

    Spacer(Modifier.height(32.dp))
    Text("Tekan lalu bicara", style = MaterialTheme.typography.titleMedium)

    Spacer(Modifier.height(32.dp))
    HintCard("Contoh", "“ingetin meeting jam 3 besok”")
}

/**
 * Tampilan 2: transkrip sudah ada dan Gemini sedang mengekstrak judul, tanggal, dan
 * jam (Fitur 2).
 *
 * Kutipan transkrip hanya ditampilkan bila tidak kosong. Tombol "Batal" mengembalikan
 * layar ke kondisi awal tanpa memanggil Gemini.
 *
 * @param transcriptText teks hasil speech-to-text yang dikutip pada kartu "Terdengar".
 * @param onCancel dipanggil saat tombol "Batal" ditekan.
 */
@Composable
private fun ProcessingContent(
    transcriptText: String,
    onCancel: () -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    CircularProgressIndicator(
        Modifier.size(56.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeWidth = 5.dp,
    )

    Spacer(Modifier.height(32.dp))
    Text("Memproses perintah", style = MaterialTheme.typography.titleMedium)

    Spacer(Modifier.height(8.dp))
    Text(
        "Mengubah suara menjadi pengingat",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (transcriptText.isNotBlank()) {
        Spacer(Modifier.height(32.dp))
        HintCard("Terdengar", "“$transcriptText”")
    }

    Spacer(Modifier.height(16.dp))
    TextButton(onClick = onCancel) { Text("Batal") }
}

/**
 * Kartu pendukung berlabel, dipakai untuk contoh perintah dan kutipan transkrip.
 *
 * Gaya kartu (warna surfaceVariant, sudut 16dp) dijadikan acuan oleh
 * ReminderResultScreen agar tampilan kedua layar konsisten.
 *
 * @param label judul kecil di atas kartu, misalnya "Contoh" atau "Terdengar".
 * @param body isi utama kartu, ditampilkan rata tengah.
 */
@Composable
private fun HintCard(label: String, body: String) = Column(
    Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
        .padding(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

/**
 * Ikon mikrofon yang digambar langsung di Canvas, supaya library
 * material-icons-extended tidak perlu ditarik hanya demi satu ikon.
 *
 * Semua ukuran dihitung relatif terhadap sisi terpendek kotak gambar (`s`), jadi ikon
 * ikut membesar atau mengecil mengikuti ukuran tombol. Terdiri dari empat bagian:
 * kapsul mikrofon, dudukan setengah lingkaran, batang, dan kaki.
 *
 * @param color warna seluruh bagian ikon.
 */
@Composable
private fun MicGlyph(color: Color, modifier: Modifier = Modifier) = Canvas(modifier.fillMaxSize()) {
    val s = minOf(size.width, size.height)
    val cx = size.width / 2f
    val line = Stroke(width = s * 0.09f, cap = StrokeCap.Round)

    // Kapsul mikrofon.
    val capsuleW = s * 0.34f
    val capsuleH = s * 0.50f
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - capsuleW / 2f, s * 0.04f),
        size = Size(capsuleW, capsuleH),
        cornerRadius = CornerRadius(capsuleW / 2f),
    )

    // Dudukan setengah lingkaran di bawah kapsul.
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - s * 0.32f, s * 0.30f),
        size = Size(s * 0.64f, s * 0.46f),
        style = line,
    )

    // Batang penyangga.
    drawLine(
        color = color,
        start = Offset(cx, s * 0.76f),
        end = Offset(cx, s * 0.92f),
        strokeWidth = line.width,
        cap = StrokeCap.Round,
    )

    // Kaki di dasar.
    drawLine(
        color = color,
        start = Offset(cx - s * 0.17f, s * 0.92f),
        end = Offset(cx + s * 0.17f, s * 0.92f),
        strokeWidth = line.width,
        cap = StrokeCap.Round,
    )
}

@Preview(name = "IDLE")
@Composable
private fun IdlePreview() = CalenVoiceTheme {
    VoiceCaptureScreen(VoiceReminderUiState(), onRecord = {}, onCancel = {})
}

@Preview(name = "PROCESSING")
@Composable
private fun ProcessingPreview() = CalenVoiceTheme {
    VoiceCaptureScreen(
        VoiceReminderUiState(
            recording = RecordingState.PROCESSING,
            transcriptText = "ingetin meeting jam 3 besok",
        ),
        onRecord = {},
        onCancel = {},
    )
}
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
 * Fitur 1 — satu layar, lima tampilan (Gambar L.1). IDLE dan PROCESSING sudah ada;
 * RECORDING, DONE dan ERROR menyusul sebagai cabang [when] di bawah.
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
    Text(
        "Reminder Suara AI",
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(Modifier.weight(1f))

    when (state.recording) {
        RecordingState.PROCESSING -> ProcessingContent(state.transcriptText, onCancel)
        else -> IdleContent(onRecord)
    }

    Spacer(Modifier.weight(1f))
}

/** Tampilan 1 — menunggu pengguna menekan mikrofon. */
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

/** Tampilan 2 — transkrip sudah ada, Gemini sedang mengekstrak judul/tanggal/jam (Fitur 2). */
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

/** Kartu pendukung berlabel — dipakai contoh perintah dan kutipan transkrip. */
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
 * Ikon mikrofon digambar langsung di Canvas — material-icons-extended tidak ditarik
 * hanya demi satu ikon. Semua ukuran relatif terhadap sisi kotak gambar.
 */
@Composable
private fun MicGlyph(color: Color, modifier: Modifier = Modifier) = Canvas(modifier.fillMaxSize()) {
    val s = minOf(size.width, size.height)
    val cx = size.width / 2f
    val line = Stroke(width = s * 0.09f, cap = StrokeCap.Round)

    val capsuleW = s * 0.34f
    val capsuleH = s * 0.50f
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - capsuleW / 2f, s * 0.04f),
        size = Size(capsuleW, capsuleH),
        cornerRadius = CornerRadius(capsuleW / 2f),
    )
    drawArc(                       // dudukan setengah lingkaran
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - s * 0.32f, s * 0.30f),
        size = Size(s * 0.64f, s * 0.46f),
        style = line,
    )
    drawLine(                      // batang
        color = color,
        start = Offset(cx, s * 0.76f),
        end = Offset(cx, s * 0.92f),
        strokeWidth = line.width,
        cap = StrokeCap.Round,
    )
    drawLine(                      // kaki
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


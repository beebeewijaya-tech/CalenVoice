package com.bee.calenvoice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bee.calenvoice.ui.theme.CalenVoiceTheme

/**
 * Fitur 1 — layar utama, tampilan IDLE (Gambar L.1 frame 1).
 * Empat tampilan lain (RECORDING, PROCESSING, DONE, ERROR) menyusul satu per satu.
 */
@Composable
fun VoiceCaptureScreen(
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Reminder Suara AI",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Text(
                "v1.0",
                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.primary)

    Spacer(Modifier.weight(1f))

    Box(
        Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(onClick = onRecord)
            .semantics { contentDescription = "Mulai merekam suara" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "MIC",
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Spacer(Modifier.height(28.dp))
    Text(
        "Tekan lalu bicara",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(20.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Contoh:", fontSize = 13.sp)
        Text("\"ingetin meeting jam 3 besok\"", fontSize = 15.sp, textAlign = TextAlign.Center)
    }

    Spacer(Modifier.weight(1f))
}

@Preview(name = "IDLE")
@Composable
private fun VoiceCaptureScreenPreview() = CalenVoiceTheme { VoiceCaptureScreen(onRecord = {}) }

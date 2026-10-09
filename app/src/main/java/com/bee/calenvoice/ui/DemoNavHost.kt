package com.bee.calenvoice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bee.calenvoice.model.RecordingState
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * ============================================================================
 *  SEMENTARA — alat uji, bukan bagian dari rancangan sistem.
 *
 *  Navigasi dummy supaya ketiga tampilan bisa dicoba di perangkat selagi
 *  ketiga repository masih TODO(). Tidak memanggil SpeechRecognizer, Gemini,
 *  maupun CalendarContract — semua perpindahan status disimulasikan di sini.
 *
 *  Hapus seluruh berkas ini begitu VoiceReminderViewModel sudah tersambung,
 *  lalu kembalikan MainActivity memanggil layar yang sesungguhnya.
 * ============================================================================
 */

private enum class Layar { IDLE, PROCESSING, HASIL }

private const val JEDA_PROSES = 1_500L   // pura-pura speech-to-text
private const val JEDA_SIMPAN = 1_200L   // pura-pura menulis ke kalender

@Composable
fun DemoNavHost(modifier: Modifier = Modifier) {
    var layar by remember { mutableStateOf(Layar.IDLE) }

    // status internal layar HASIL
    var sedangMenyimpan by remember { mutableStateOf(false) }
    var berhasil by remember { mutableStateOf(false) }
    var pesanGagal by remember { mutableStateOf<String?>(null) }

    // sakelar uji: kalau menyala, tombol Simpan sengaja digagalkan
    var paksaGagal by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val contoh = remember { Reminder(title = "Meeting", date = "2026-10-10", time = "15:00") }

    fun resetHasil() {
        sedangMenyimpan = false
        berhasil = false
        pesanGagal = null
    }

    // PROCESSING tidak punya tombol maju — ia berjalan sendiri lalu pindah ke HASIL,
    // persis seperti alur sebenarnya nanti (FR-04 selesai → lanjut Fitur 2).
    LaunchedEffect(layar) {
        if (layar == Layar.PROCESSING) {
            delay(JEDA_PROSES)
            layar = Layar.HASIL
        }
    }

    // Disusun, bukan ditumpuk: panel uji mengambil jatah ruangnya sendiri supaya
    // tidak pernah menutupi tombol di bagian bawah layar.
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (layar) {
                Layar.IDLE -> VoiceCaptureScreen(
                    state = VoiceReminderUiState(),
                    onRecord = { layar = Layar.PROCESSING },
                    onCancel = {},
                )

                Layar.PROCESSING -> VoiceCaptureScreen(
                    state = VoiceReminderUiState(
                        recording = RecordingState.PROCESSING,
                        transcriptText = "ingetin aku meeting jam 3 besok",
                    ),
                    onRecord = {},
                    onCancel = { layar = Layar.IDLE },
                )

                Layar.HASIL -> ReminderResultScreen(
                    reminder = contoh,
                    isLoading = sedangMenyimpan,
                    errorMessage = pesanGagal,
                    isSuccess = berhasil,
                    onSaveClicked = {
                        scope.launch {
                            pesanGagal = null
                            sedangMenyimpan = true
                            delay(JEDA_SIMPAN)
                            sedangMenyimpan = false
                            if (paksaGagal) {
                                pesanGagal = "Izin akses kalender belum diberikan di pengaturan perangkat."
                            } else {
                                berhasil = true
                            }
                        }
                    },
                    onRetryClicked = { resetHasil() },
                    onDoneClicked = {
                        resetHasil()
                        layar = Layar.IDLE
                    },
                )
            }
        }

        PanelUji(
            layar = layar,
            paksaGagal = paksaGagal,
            onPilihLayar = { resetHasil(); layar = it },
            onUbahPaksaGagal = { paksaGagal = it },
        )
    }
}

/** Baris pintasan supaya tiap tampilan bisa dibuka langsung tanpa mengulang alur. */
@Composable
private fun PanelUji(
    layar: Layar,
    paksaGagal: Boolean,
    onPilihLayar: (Layar) -> Unit,
    onUbahPaksaGagal: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.secondary)
        .navigationBarsPadding()
        .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
) {
    Layar.entries.forEach { tujuan ->
        TextButton(onClick = { onPilihLayar(tujuan) }) {
            Text(
                text = tujuan.name,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (layar == tujuan) FontWeight.Bold else FontWeight.Normal,
                color = if (layar == tujuan) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    TextButton(onClick = { onUbahPaksaGagal(!paksaGagal) }) {
        Text(
            text = if (paksaGagal) "GAGAL ✓" else "GAGAL",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (paksaGagal) FontWeight.Bold else FontWeight.Normal,
            color = if (paksaGagal) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

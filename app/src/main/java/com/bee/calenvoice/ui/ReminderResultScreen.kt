package com.bee.calenvoice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bee.calenvoice.model.Reminder
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun ReminderResultScreen(
    reminder: Reminder?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isSuccess: Boolean = false,
    onSaveClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onDoneClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminder Suara AI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black
                )
            }

            // --- KONTEN UTAMA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        // Loading State (Proses Menyimpan)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(2.dp, Color.Black), RoundedCornerShape(12.dp))
                                .padding(32.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Menyimpan ke kalender...",
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    isSuccess -> {
                        // Status Berhasil Simpan
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color.Black),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "[✓]",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Acara berhasil disimpan!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                reminder?.let {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = it.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${it.date} • ${it.time}",
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    errorMessage != null -> {
                        // Status Gagal / Error State (Alur Alternatif D)
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color.Black),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "!",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "BELUM BERHASIL DISIMPAN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    reminder != null -> {
                        // Tampilan Draf Hasil Gemini AI (Sesuai Layout Bee)
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color.Black),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Rencana Terdeteksi:",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = reminder.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "📅 ${reminder.date}",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "⏰ ${reminder.time}",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- TOMBOL AKSI UTAMA ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                when {
                    isSuccess -> {
                        Button(
                            onClick = onDoneClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "SELESAI",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    errorMessage != null -> {
                        Button(
                            onClick = onRetryClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "COBA LAGI",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = onSaveClicked,
                            enabled = !isLoading && reminder != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                disabledContainerColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "SIMPAN KE KALENDER",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true, name = "1. Tampilan Draf Hasil Gemini")
@Composable
fun ReminderResultScreenPreview() {
    ReminderResultScreen(
        reminder = Reminder(
            title = "Meeting Proyek CalenVoice",
            date = "2026-10-09",
            time = "15:00"
        ),
        isLoading = false,
        errorMessage = null,
        isSuccess = false,
        onSaveClicked = {},
        onRetryClicked = {},
        onDoneClicked = {}
    )
}

@Preview(showBackground = true, name = "2. Tampilan Loading Simpan")
@Composable
fun ReminderResultScreenLoadingPreview() {
    ReminderResultScreen(
        reminder = Reminder(
            title = "Meeting Proyek CalenVoice",
            date = "2026-10-09",
            time = "15:00"
        ),
        isLoading = true,
        errorMessage = null,
        isSuccess = false,
        onSaveClicked = {},
        onRetryClicked = {},
        onDoneClicked = {}
    )
}

@Preview(showBackground = true, name = "3. Tampilan Berhasil Simpan")
@Composable
fun ReminderResultScreenSuccessPreview() {
    ReminderResultScreen(
        reminder = Reminder(
            title = "Meeting Proyek CalenVoice",
            date = "2026-10-09",
            time = "15:00"
        ),
        isLoading = false,
        errorMessage = null,
        isSuccess = true,
        onSaveClicked = {},
        onRetryClicked = {},
        onDoneClicked = {}
    )
}

@Preview(showBackground = true, name = "4. Tampilan Gagal Simpan")
@Composable
fun ReminderResultScreenErrorPreview() {
    ReminderResultScreen(
        reminder = null,
        isLoading = false,
        errorMessage = "Izin akses kalender belum diberikan di pengaturan perangkat.",
        isSuccess = false,
        onSaveClicked = {},
        onRetryClicked = {},
        onDoneClicked = {}
    )
}
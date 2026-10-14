package com.bee.calenvoice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bee.calenvoice.data.CalendarPermissionException
import com.bee.calenvoice.data.CalendarRepository
import com.bee.calenvoice.data.InferenceRepository
import com.bee.calenvoice.data.InvalidReminderException
import com.bee.calenvoice.data.NoWritableCalendarException
import com.bee.calenvoice.data.SpeechRepository
import com.bee.calenvoice.model.RecordingState
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State yang turun dari ViewModel ke View sebagai StateFlow (MVVM, Gambar 2.2). */
data class VoiceReminderUiState(
    val recording: RecordingState = RecordingState.IDLE,
    val elapsedSeconds: Int = 0,
    val transcriptText: String = "",
    val reminder: Reminder? = null,
    val savedEventId: Long? = null,
    val error: String? = null,
    /** Fitur 3: true selama event sedang ditulis ke kalender (layar loading). */
    val isSaving: Boolean = false,
    /** Fitur 3: sinyal ke View supaya memunculkan dialog izin kalender. Di-reset setelah dipakai. */
    val needsCalendarPermission: Boolean = false,
    /** Fitur 3: true jika error karena izin, supaya View menampilkan tombol ke Pengaturan. */
    val errorIsPermission: Boolean = false,
) {
    companion object {
        const val MAX_SECONDS = 60 // FR-03
    }
}

/**
 * Lapisan 1: pemegang urutan langkah dan seluruh keputusan alur (Tabel 2.2).
 * Murni Kotlin: tidak menyentuh API Android langsung.
 */
class VoiceReminderViewModel(
    private val speech: SpeechRepository,
    private val inference: InferenceRepository,
    private val calendar: CalendarRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceReminderUiState())
    val uiState: StateFlow<VoiceReminderUiState> = _uiState.asStateFlow()

    /** Fitur 1 → validasi regex → Fitur 2 (Gambar 4.1 dan 4.2). */
    fun onRecordClicked() {
        _uiState.update {
            it.copy(
                recording = RecordingState.RECORDING,
                elapsedSeconds = 0,
                transcriptText = "",
                reminder = null,
                savedEventId = null,
                error = null
            )
        }
    }

    /** Berhenti lebih awal; auto-stop di detik 60 juga lewat sini. */
    fun onStopClicked() {
        // Mengubah status perekaman ke IDLE atau memanggil fungsi penghentian perekaman
        _uiState.update { it.copy(recording = RecordingState.IDLE) }
    }

    /** Batal: kembali ke IDLE tanpa memanggil Gemini. */
    fun onCancelClicked() {
        _uiState.update { VoiceReminderUiState() }
    }

    // ------------------------------------------------------------------
    // Fitur 3: Save Calendar (Gambar 4.3)
    // ------------------------------------------------------------------

    /**
     * Simpan draf yang sedang tampil ke kalender perangkat.
     *
     * Urutan keputusan (semua keputusan alur ada di ViewModel, Tabel 2.2):
     * 1. Kalau izin kalender belum ada, minta View menampilkan dialog izin dulu.
     * 2. Kalau sudah ada, panggil CalendarRepository.createEvent di viewModelScope.
     * 3. Hasilnya dipetakan ke state: sukses mengisi savedEventId, gagal mengisi error.
     */
    fun onSaveClicked() {
        val currentReminder = _uiState.value.reminder ?: return
        if (_uiState.value.isSaving) return // cegah double tap

        if (!calendar.hasCalendarPermission()) {
            _uiState.update {
                it.copy(needsCalendarPermission = true, error = null, errorIsPermission = false)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, errorIsPermission = false) }

            calendar.createEvent(currentReminder)
                .onSuccess { eventId ->
                    _uiState.update { it.copy(isSaving = false, savedEventId = eventId) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.toUserMessage(),
                            errorIsPermission = e is CalendarPermissionException,
                        )
                    }
                }
        }
    }

    /** Dipanggil View tepat setelah dialog izin ditampilkan, supaya sinyalnya tidak terpicu ulang. */
    fun onCalendarPermissionRequestHandled() {
        _uiState.update { it.copy(needsCalendarPermission = false) }
    }

    /**
     * Hasil dialog izin dari View. Kalau diizinkan, lanjut menyimpan otomatis.
     * Kalau ditolak, masuk Alur Alternatif D: tampilkan pesan dan tawarkan buka Pengaturan.
     */
    fun onCalendarPermissionResult(granted: Boolean) {
        if (granted) {
            onSaveClicked()
        } else {
            _uiState.update {
                it.copy(error = CalendarPermissionException().toUserMessage(), errorIsPermission = true)
            }
        }
    }

    /** Tombol "Selesai" di layar sukses: kembali ke kondisi awal untuk reminder berikutnya. */
    fun onDoneClicked() {
        _uiState.update { VoiceReminderUiState() }
    }

    /** Milik Bee: reset total ke kondisi awal (reminder ikut hilang). */
    fun onRetry() {
        _uiState.update { VoiceReminderUiState() }
    }

    /** Menerjemahkan exception dari lapisan data menjadi pesan yang bisa dibaca pengguna (FR-12). */
    private fun Throwable.toUserMessage(): String = when (this) {
        is CalendarPermissionException ->
            "Event tidak bisa disimpan karena izin kalender belum diberikan."
        is NoWritableCalendarException ->
            "Tidak ada kalender di HP ini. Tambahkan akun Google atau kalender dulu, lalu coba lagi."
        is InvalidReminderException ->
            "Tanggal atau jam hasil rekaman tidak valid. Coba rekam ulang."
        else ->
            "Event tidak bisa disimpan. Coba lagi sebentar."
    }

}
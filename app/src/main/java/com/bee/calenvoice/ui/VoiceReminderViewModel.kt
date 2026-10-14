package com.bee.calenvoice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bee.calenvoice.data.CalendarPermissionException
import com.bee.calenvoice.data.CalendarRepository
import com.bee.calenvoice.data.InferenceRepository
import com.bee.calenvoice.data.InvalidReminderException
import com.bee.calenvoice.data.NoSpeechException
import com.bee.calenvoice.data.NoWritableCalendarException
import com.bee.calenvoice.data.SpeechNetworkException
import com.bee.calenvoice.data.SpeechPermissionException
import com.bee.calenvoice.data.SpeechRepository
import com.bee.calenvoice.data.SpeechUnavailableException
import com.bee.calenvoice.model.RecordingState
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    /** Fitur 1: sinyal ke View supaya memunculkan dialog izin mikrofon. Di-reset setelah dipakai. */
    val needsMicPermission: Boolean = false,
    /** true jika error karena izin, supaya View menampilkan tombol ke Pengaturan. */
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

    /** Sesi rekam + inference yang sedang berjalan, dipegang supaya Batal bisa menghentikannya. */
    private var captureJob: Job? = null

    // ------------------------------------------------------------------
    // Fitur 1 dan 2: rekam, validasi, lalu serahkan ke Gemini (Gambar 4.1 dan 4.2)
    // ------------------------------------------------------------------

    /**
     * Satu alur utuh dari tombol rekam sampai Reminder siap disimpan.
     *
     * Urutannya persis Gambar 4.1 dan 4.2, dan semua percabangannya ada di sini karena
     * ViewModel yang memegang keputusan alur (Tabel 2.2):
     *
     * 1. Izin mikrofon belum ada, minta View menampilkan dialog dulu (Alur Alternatif B).
     * 2. Rekam sampai pengguna diam atau 60 detik terlewati (FR-02, FR-03, FR-04).
     * 3. Teks kosong, berhenti dengan pesan (FR-05).
     * 4. Tidak ada kata "jam" atau "pukul", minta ucap ulang (FR-06, Alur Alternatif A).
     * 5. Lolos, status Processing lalu teks diserahkan ke Gemini (FR-08, FR-09).
     * 6. Gemini gagal, tawarkan Coba Lagi (Alur Alternatif C).
     */
    fun onRecordClicked() {
        if (captureJob?.isActive == true) return // cegah double tap

        if (!speech.hasMicPermission()) {
            _uiState.update {
                it.copy(needsMicPermission = true, error = null, errorIsPermission = false)
            }
            return
        }

        captureJob = viewModelScope.launch {
            _uiState.value = VoiceReminderUiState(recording = RecordingState.RECORDING)
            val ticker = launch { tickElapsedSeconds() }

            val transcript = try {
                speech.listen(VoiceReminderUiState.MAX_SECONDS)
            } finally {
                ticker.cancel()
            }

            transcript
                .onFailure { e -> fail(e) }
                .onSuccess { result ->
                    when {
                        // FR-05: tidak ada yang terdengar.
                        result.isEmpty() -> fail(NoSpeechException())

                        // FR-06: ucapan harus menyebut jam atau pukul (Alur Alternatif A).
                        !result.hasTimeKeyword() -> _uiState.update {
                            it.copy(
                                recording = RecordingState.ERROR,
                                transcriptText = result.text,
                                error = "Ucapan harus menyebut \"jam\" atau \"pukul\". Coba rekam ulang.",
                            )
                        }

                        else -> extract(result.text)
                    }
                }
        }
    }

    /** Fitur 2: teks yang sudah lolos validasi diserahkan ke Gemini. */
    private suspend fun extract(text: String) {
        _uiState.update {
            it.copy(recording = RecordingState.PROCESSING, transcriptText = text, error = null)
        }

        inference.extract(text, System.currentTimeMillis())
            .onSuccess { reminder ->
                // Reminder terisi, sehingga VoiceReminderRoute otomatis berpindah ke layar hasil.
                _uiState.update { it.copy(recording = RecordingState.DONE, reminder = reminder) }
            }
            .onFailure { e -> fail(e) }
    }

    /**
     * Menaikkan penghitung detik selama merekam, supaya UI bisa menampilkan 00:07 dan seterusnya.
     * Tidak perlu memeriksa isActive sendiri: delay() sudah melempar saat coroutine dibatalkan.
     */
    private suspend fun tickElapsedSeconds() {
        while (true) {
            delay(1_000)
            _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        }
    }

    /** Berhenti lebih awal. Hasil sementara tetap diproses, jadi bukan pembatalan. */
    fun onStopClicked() {
        speech.stopListening()
    }

    /** Batal: hentikan sesi dan kembali ke kondisi awal tanpa memanggil Gemini. */
    fun onCancelClicked() {
        captureJob?.cancel()
        captureJob = null
        speech.stopListening()
        _uiState.value = VoiceReminderUiState()
    }

    /** Dipanggil View tepat setelah dialog izin mikrofon ditampilkan. */
    fun onMicPermissionRequestHandled() {
        _uiState.update { it.copy(needsMicPermission = false) }
    }

    /** Hasil dialog izin mikrofon. Kalau diizinkan, langsung mulai merekam. */
    fun onMicPermissionResult(granted: Boolean) {
        if (granted) {
            onRecordClicked()
        } else {
            _uiState.update {
                it.copy(
                    recording = RecordingState.ERROR,
                    error = SpeechPermissionException().toUserMessage(),
                    errorIsPermission = true,
                )
            }
        }
    }

    /** Memindahkan state ke ERROR dengan pesan yang sudah diterjemahkan. */
    private fun fail(e: Throwable) {
        _uiState.update {
            it.copy(
                recording = RecordingState.ERROR,
                error = e.toUserMessage(),
                errorIsPermission = e is SpeechPermissionException,
            )
        }
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
        _uiState.value = VoiceReminderUiState()
    }

    /** Reset total ke kondisi awal (reminder ikut hilang). */
    fun onRetry() {
        _uiState.value = VoiceReminderUiState()
    }

    /** Menerjemahkan exception dari lapisan data menjadi pesan yang bisa dibaca pengguna. */
    private fun Throwable.toUserMessage(): String = when (this) {
        // Fitur 1
        is SpeechPermissionException ->
            "Izin mikrofon belum diberikan. Aktifkan dulu lewat Pengaturan."
        is SpeechUnavailableException ->
            "Perangkat ini belum punya layanan ubah suara ke teks. Coba di HP asli, bukan emulator."
        is NoSpeechException ->
            "Suara tidak terdengar. Coba rekam ulang dan bicara lebih dekat ke mikrofon."
        is SpeechNetworkException ->
            "Layanan ubah suara ke teks butuh internet dan sedang tidak bisa dihubungi. " +
                    "Periksa koneksi lalu coba lagi."

        // Fitur 3
        is CalendarPermissionException ->
            "Event tidak bisa disimpan karena izin kalender belum diberikan."
        is NoWritableCalendarException ->
            "Tidak ada kalender di HP ini. Tambahkan akun Google atau kalender dulu, lalu coba lagi."
        is InvalidReminderException ->
            "Tanggal atau jam hasil rekaman tidak valid. Coba rekam ulang."

        else ->
            "Terjadi kesalahan. Coba lagi sebentar."
    }
}

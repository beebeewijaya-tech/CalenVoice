package com.bee.calenvoice.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.bee.calenvoice.model.Transcript
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.resume

/**
 * Lapisan 2 (Data Layer), Fitur 1: Voice Recording dan Speech-to-Text.
 *
 * Abstraksi ini yang membuat ViewModel bisa diuji tanpa perangkat (Tabel 2.4): saat unit
 * test, SpeechRepository diganti versi palsu yang langsung mengembalikan Transcript.
 *
 * Catatan arsitektur (Layered + MVVM):
 * Sama seperti CalendarRepository, repository ini tidak boleh menampilkan dialog izin
 * karena itu urusan UI. Repository hanya menyediakan hasMicPermission(); dialognya
 * diluncurkan di lapisan UI (lihat VoiceReminderRoute).
 */
interface SpeechRepository {

    /** @return true jika RECORD_AUDIO sudah diberikan pengguna. */
    fun hasMicPermission(): Boolean

    /** @return true jika perangkat punya layanan speech-to-text sama sekali. */
    fun isAvailable(): Boolean

    /**
     * Merekam suara lalu mengubahnya menjadi teks (FR-02, FR-03, FR-04).
     *
     * Berhenti sendiri saat pengguna diam, atau paling lama setelah [maxSeconds] detik
     * sesuai FR-03. Fungsi ini menahan alur sampai hasilnya datang.
     *
     * @return Result sukses berisi [Transcript] beserta nilai confidence-nya. Result gagal
     * berisi [SpeechPermissionException], [SpeechUnavailableException], [NoSpeechException],
     * atau exception lain dari sistem.
     */
    suspend fun listen(maxSeconds: Int = 60): Result<Transcript>

    /** Menghentikan perekaman lebih awal. Hasil sementara tetap dikembalikan lewat [listen]. */
    fun stopListening()
}

/** Izin mikrofon belum diberikan atau dicabut. Memicu UC-01 Alur Alternatif B. */
class SpeechPermissionException : Exception("Izin mikrofon belum diberikan")

/** Perangkat tidak punya layanan speech-to-text (sering terjadi di emulator polos). */
class SpeechUnavailableException : Exception("Perangkat ini tidak punya layanan ubah suara ke teks")

/** Tidak ada suara yang tertangkap, atau hasilnya kosong. Memicu FR-05. */
class NoSpeechException : Exception("Suara tidak terdengar")

/**
 * Layanan pengenalan suara tidak bisa menghubungi servernya.
 *
 * Penting dibedakan dari NoInternetException milik Fitur 2: yang gagal di sini adalah
 * layanan Google di dalam perangkat, bukan panggilan aplikasi ke Gemini. Sering muncul
 * di emulator yang jaringannya terbatas.
 */
class SpeechNetworkException : Exception("Layanan ubah suara ke teks tidak bisa dihubungi")

/**
 * Implementasi SpeechRepository memakai android.speech.SpeechRecognizer.
 *
 * Dua hal yang menentukan bentuk kode di bawah:
 *
 * 1. SpeechRecognizer wajib dibuat dan dipanggil dari main thread. Karena itu seluruh
 *    interaksi dengannya dibungkus withContext(Dispatchers.Main).
 * 2. SpeechRecognizer berbasis callback, sedangkan alur di ViewModel berbasis suspend.
 *    suspendCancellableCoroutine menjembatani keduanya: callback pertama yang masuk
 *    me-resume continuation, sisanya diabaikan lewat penjaga isActive.
 */
class AndroidSpeechRepository(
    private val context: Context,
    /** Bahasa pengenalan. Dipisah supaya gampang diganti saat uji coba. */
    private val language: String = "id-ID",
) : SpeechRepository {

    /** Dipegang supaya stopListening() bisa menyentuh sesi yang sedang berjalan. */
    private var recognizer: SpeechRecognizer? = null

    override fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override suspend fun listen(maxSeconds: Int): Result<Transcript> {
        // Dua penjagaan di depan supaya kegagalan yang bisa diprediksi tidak sampai
        // membuka sesi perekaman sama sekali.
        if (!hasMicPermission()) return Result.failure(SpeechPermissionException())
        if (!isAvailable()) return Result.failure(SpeechUnavailableException())

        return withContext(Dispatchers.Main) {
            runCatching {
                coroutineScope {
                    // Penjaga FR-03: setelah maxSeconds, perekaman dihentikan secara halus
                    // lewat stopListening() supaya hasil sementara tetap dikembalikan,
                    // bukan dibatalkan begitu saja.
                    val timer = launch {
                        delay(maxSeconds * 1_000L)
                        stopListening()
                    }
                    try {
                        awaitRecognition()
                    } finally {
                        timer.cancel()
                        release()
                    }
                }
            }
        }
    }

    override fun stopListening() {
        runCatching { recognizer?.stopListening() }
    }

    /** Membuka satu sesi pengenalan dan menunggu callback pertama yang menentukan hasilnya. */
    private suspend fun awaitRecognition(): Transcript = suspendCancellableCoroutine { cont ->
        val speech = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }

        speech.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()

                if (text.isBlank()) {
                    cont.failWith(NoSpeechException())
                    return
                }

                // Sebagian layanan tidak mengirim skor sama sekali. Ketika itu terjadi,
                // 1f dipakai supaya UI tidak salah menuduh hasilnya meragukan.
                val confidence = results
                    ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    ?.firstOrNull()
                    ?.takeIf { it > 0f }
                    ?: 1f

                if (cont.isActive) cont.resume(Transcript(text, confidence))
            }

            override fun onError(error: Int) = cont.failWith(error.toException())

            // Callback sisanya tidak dipakai: UI perekaman di aplikasi ini tidak
            // menampilkan meteran volume, jadi tidak ada yang perlu ditindaklanjuti.
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        speech.startListening(buildIntent(language))

        // Kalau ViewModel membatalkan (pengguna menekan Batal), sesi ikut ditutup.
        cont.invokeOnCancellation { release() }
    }

    private fun release() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun CancellableContinuation<Transcript>.failWith(e: Throwable) {
        if (isActive) resumeWith(Result.failure(e))
    }

    /** Memetakan kode error SpeechRecognizer ke exception milik lapisan data. */
    private fun Int.toException(): Throwable = when (this) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechPermissionException()
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NoSpeechException()
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER -> SpeechNetworkException()
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        SpeechRecognizer.ERROR_CLIENT -> SpeechUnavailableException()
        else -> Exception("Perekaman gagal (kode $this)")
    }

    private companion object {
        fun buildIntent(language: String) =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Minta skor keyakinan; dipakai Transcript.confidence (Gambar 3.1).
                putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
                // Diam 2 detik dianggap ucapan sudah selesai, supaya pengguna tidak
                // perlu menekan tombol stop untuk kalimat pendek.
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    2_000L,
                )
            }
    }
}

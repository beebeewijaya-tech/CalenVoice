package com.bee.calenvoice.data

import com.bee.calenvoice.BuildConfig
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lapisan 2 (Data Layer), Fitur 2: Inference Gemini.
 *
 * Ini satu-satunya bagian aplikasi yang keluar ke internet, jadi satu-satunya tempat
 * pola Client-Server berlaku (Gambar 2.3). Aplikasi berperan sebagai klien, Gemini API
 * sebagai server, konektornya request/reply lewat HTTPS. Konsekuensinya juga disadari:
 * server adalah titik tunggal kegagalan, dan kalau internet mati alur jatuh ke
 * Alur Alternatif C.
 */
interface InferenceRepository {

    /**
     * Menyerahkan [text] hasil transkrip ke Gemini untuk diubah menjadi data terstruktur
     * berisi judul, tanggal, dan jam (FR-08, FR-09).
     *
     * @param now waktu sekarang dalam epoch millis. Wajib dikirim supaya Gemini bisa
     * menghitung kata relatif seperti "besok" menjadi tanggal pasti (FR-08).
     *
     * @return Result sukses berisi [Reminder] yang tanggal dan jamnya sudah lolos
     * pemeriksaan format. Result gagal berisi [MissingApiKeyException],
     * [NoInternetException], [GeminiFormatException], atau [GeminiException].
     */
    suspend fun extract(text: String, now: Long): Result<Reminder>
}

/** GEMINI_API_KEY belum diisi di local.properties. */
class MissingApiKeyException : Exception("Kunci Gemini belum diisi di local.properties")

/** Tidak bisa menghubungi Gemini: internet mati atau server tidak menjawab. */
class NoInternetException : Exception("Tidak bisa menghubungi Gemini")

/** Gemini menjawab, tetapi isinya tidak sesuai format yang diminta (FR-09). */
class GeminiFormatException(message: String) : Exception(message)

/** Gemini menolak permintaan, misalnya kunci salah atau kuota habis. */
open class GeminiException(message: String) : Exception(message)

/**
 * Gemini sedang sibuk atau bermasalah (HTTP 429 dan 5xx). Dibedakan karena sifatnya
 * sementara: permintaan yang sama bisa berhasil beberapa detik kemudian, jadi layak
 * diulang alih-alih langsung dinyatakan gagal.
 */
class GeminiBusyException(message: String) : GeminiException(message)

/**
 * Menjalankan [blok], mengulanginya selama ia melempar [GeminiBusyException].
 *
 * Sengaja dipisah dari urusan jaringan supaya perilakunya bisa diuji tanpa server:
 * jumlah percobaan, jeda yang menaik, dan exception terakhir yang dilempar kalau
 * semua percobaan habis.
 *
 * @param maksPercobaan total percobaan, termasuk yang pertama.
 * @param jedaMs jeda dasar; percobaan ke-n menunggu jedaMs * n.
 */
internal suspend fun <T> ulangiSaatSibuk(
    maksPercobaan: Int,
    jedaMs: Long,
    blok: () -> T,
): T {
    var terakhir: GeminiBusyException? = null
    repeat(maksPercobaan) { ke ->
        try {
            return blok()
        } catch (e: GeminiBusyException) {
            terakhir = e
            if (ke < maksPercobaan - 1) delay(jedaMs * (ke + 1))
        }
    }
    throw terakhir ?: IllegalStateException("maksPercobaan harus minimal 1")
}

/**
 * Implementasi InferenceRepository memakai REST API Gemini lewat HttpURLConnection.
 *
 * Sengaja tanpa SDK maupun pustaka HTTP tambahan: yang dibutuhkan hanya satu POST JSON,
 * dan HttpURLConnection beserta org.json sudah tersedia di Android.
 *
 * Keluaran Gemini dikunci memakai responseSchema sehingga balasannya dijamin berbentuk
 * JSON {title, date, time}. Ini jauh lebih aman daripada meminta JSON lewat kalimat
 * prompt lalu berharap modelnya menurut.
 */
class GeminiInferenceRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val model: String = DEFAULT_MODEL,
) : InferenceRepository {

    override suspend fun extract(text: String, now: Long): Result<Reminder> {
        if (apiKey.isBlank()) return Result.failure(MissingApiKeyException())

        return withContext(Dispatchers.IO) {
            runCatching {
                val body = requestBody(text, now)
                val raw = post(body)
                parseReminder(raw)
            }.recoverCatching { e ->
                // IOException berarti permintaannya tidak sampai: internet mati, DNS gagal,
                // atau koneksi putus di tengah jalan. Dibedakan dari kegagalan lain supaya
                // pesan ke pengguna bisa tepat.
                throw if (e is IOException) NoInternetException() else e
            }
        }
    }

    // ------------------------------------------------------------------
    // Menyusun permintaan
    // ------------------------------------------------------------------

    private fun requestBody(text: String, now: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val nowText = fmt.format(Date(now))

        val prompt = """
            Kamu mengubah satu kalimat bahasa Indonesia menjadi data pengingat.

            Waktu sekarang: $nowText
            Gunakan waktu sekarang itu untuk menghitung kata relatif seperti
            "besok", "lusa", "nanti", atau nama hari.

            Aturan:
            - title: inti kegiatannya saja, singkat, tanpa menyebut waktu.
            - date: format yyyy-MM-dd.
            - time: format HH:mm, 24 jam. "jam 3 sore" berarti 15:00.
            - Jangan pernah menebak jam. Kalimat yang masuk ke sini sudah dipastikan
              menyebut jam beserta angkanya, jadi pakai angka itu apa adanya.
            - Kalau tanggal tidak disebut sama sekali, pakai tanggal hari ini.

            Kalimatnya: "$text"
        """.trimIndent()

        // responseSchema mengunci bentuk balasan, jadi tidak perlu membersihkan
        // pagar ```json yang biasa muncul kalau JSON hanya diminta lewat prompt.
        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties", JSONObject()
                    .put("title", JSONObject().put("type", "STRING"))
                    .put("date", JSONObject().put("type", "STRING"))
                    .put("time", JSONObject().put("type", "STRING"))
            )
            .put("required", listOf("title", "date", "time").toJsonArray())

        return JSONObject()
            .put(
                "contents", listOf(
                    JSONObject().put(
                        "parts", listOf(JSONObject().put("text", prompt)).toJsonArray()
                    )
                ).toJsonArray()
            )
            .put(
                "generationConfig", JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", schema)
                    .put("temperature", 0)
                    // Catatan: thinkingConfig sengaja TIDAK dikirim. Model lite menolaknya
                    // dengan HTTP 400 "invalid argument", dan memang tidak diperlukan karena
                    // model ini sudah menjawab sekitar satu detik tanpa tahap berpikir.
            )
            .toString()
    }

    /**
     * Mengirim permintaan, mengulang bila Gemini sedang sibuk.
     *
     * Hanya kegagalan sementara (429 dan 5xx) yang diulang. Kunci salah, model tidak
     * ditemukan, dan kesalahan bentuk permintaan tidak akan membaik dengan diulang,
     * jadi langsung dilempar supaya pengguna cepat tahu.
     */
    private suspend fun post(body: String): String =
        ulangiSaatSibuk(MAX_PERCOBAAN, JEDA_ULANG_MS) { postOnce(body) }

    private fun postOnce(body: String): String {
        val url = URL("$ENDPOINT/$model:generateContent")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
            // Kunci dikirim lewat header, bukan query string, supaya tidak ikut
            // tercatat di log proxy atau riwayat URL.
            setRequestProperty("x-goog-api-key", apiKey)
        }

        return try {
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val pesan = geminiErrorMessage(code, detail)
                throw if (code == 429 || code in 500..599) GeminiBusyException(pesan)
                else GeminiException(pesan)
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ------------------------------------------------------------------
    // Membaca balasan (FR-09)
    // ------------------------------------------------------------------

    private fun parseReminder(raw: String): Reminder {
        val payload = runCatching {
            JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }.getOrElse {
            throw GeminiFormatException("Balasan Gemini tidak bisa dibaca")
        }

        val obj = runCatching { JSONObject(payload) }.getOrElse {
            throw GeminiFormatException("Isi balasan Gemini bukan JSON")
        }

        val title = obj.optString("title").trim()
        val date = obj.optString("date").trim()
        val time = obj.optString("time").trim()

        // Pemeriksaan kelengkapan dan format sebelum Reminder diteruskan ke Fitur 3.
        // Lebih baik gagal di sini daripada baru ketahuan saat menulis ke kalender.
        if (title.isEmpty()) throw GeminiFormatException("Judul pengingat tidak terbaca")
        if (!DATE_PATTERN.matches(date)) throw GeminiFormatException("Tanggal tidak terbaca")
        if (!TIME_PATTERN.matches(time)) throw GeminiFormatException("Jam tidak terbaca")

        return Reminder(title = title, date = date, time = time)
    }

    private fun geminiErrorMessage(code: Int, detail: String): String {
        val fromServer = runCatching {
            JSONObject(detail).getJSONObject("error").getString("message")
        }.getOrNull()
        return when (code) {
            400, 403 -> "Kunci Gemini ditolak: ${fromServer ?: "periksa GEMINI_API_KEY"}"
            401 -> "Kunci Gemini tidak berlaku. Pakai API key dari aistudio.google.com (diawali AIza)."
            404 -> fromServer
                ?: "Model \"$model\" tidak tersedia untuk kunci ini, atau kuota kunci sudah habis."
            429 -> "Kuota Gemini habis, coba lagi nanti"
            in 500..599 -> "Server Gemini sedang bermasalah"
            else -> fromServer ?: "Gemini menolak permintaan (kode $code)"
        }
    }

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"

        /**
         * Model yang dipakai. Gampang diganti lewat parameter konstruktor kalau perlu.
         *
         * Riwayat pemilihannya, supaya tidak diulang dari nol:
         * - gemini-2.0-flash ditolak server, "no longer available".
         * - gemini-3.8-flash berulang kali dijawab 503 high demand.
         * - gemini-flash-latest sempat berhasil, tetapi makan 75 detik karena berpikir dulu.
         * - gemini-3.5-flash-lite menjawab benar dalam 1 detik. Dipilih.
         *
         * Alternatif kalau model ini bermasalah: "gemini-flash-lite-latest" atau
         * "gemini-flash-latest". Perhatikan model lite menolak thinkingConfig.
         */
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

        /**
         * Model lite menjawab sekitar satu detik pada pengujian, jadi 30 detik sudah
         * sangat longgar sekaligus cukup cepat menyerah ketika jaringan bermasalah.
         *
         * GeminiLiveTest mengukur dan melaporkan lamanya setiap kali dijalankan, jadi
         * kalau suatu saat melambat, angkanya akan terlihat.
         */
        const val TIMEOUT_MS = 30_000

        /** Tiga percobaan dengan jeda 1 lalu 2 detik; cukup melewati lonjakan beban sesaat. */
        const val MAX_PERCOBAAN = 3
        const val JEDA_ULANG_MS = 1_000L

        val DATE_PATTERN = Regex("""\d{4}-\d{2}-\d{2}""")
        val TIME_PATTERN = Regex("""\d{2}:\d{2}""")

        fun List<Any>.toJsonArray() = org.json.JSONArray(this)
    }
}

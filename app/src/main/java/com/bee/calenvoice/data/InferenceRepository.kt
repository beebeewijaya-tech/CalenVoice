package com.bee.calenvoice.data

import com.bee.calenvoice.BuildConfig
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.Dispatchers
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
class GeminiException(message: String) : Exception(message)

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
            - Kalau jam tidak disebut sama sekali, pakai 09:00.
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
                    // Mematikan tahap berpikir model. Mengambil tiga field dari satu
                    // kalimat tidak butuh penalaran bertingkat, sementara biayanya besar:
                    // satu panggilan uji dengan thinking menyala memakan 75 detik.
                    .put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
            )
            .toString()
    }

    private fun post(body: String): String {
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

            if (conn.responseCode !in 200..299) {
                val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw GeminiException(geminiErrorMessage(conn.responseCode, detail))
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
            429 -> "Kuota Gemini habis, coba lagi nanti"
            in 500..599 -> "Server Gemini sedang bermasalah"
            else -> fromServer ?: "Gemini menolak permintaan (kode $code)"
        }
    }

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"

        /**
         * Sengaja memakai alias "-latest", bukan nomor versi.
         *
         * Model bernomor dipensiunkan: gemini-2.0-flash yang semula dipakai di sini sudah
         * ditolak server dengan pesan "no longer available". Alias ini dijaga Google supaya
         * selalu menunjuk ke flash yang masih hidup, jadi aplikasi tidak mati sendiri
         * beberapa bulan setelah dikumpulkan.
         */
        const val DEFAULT_MODEL = "gemini-flash-latest"

        /**
         * Longgar karena flash terbaru berpikir dulu sebelum menjawab: satu panggilan uji
         * dengan prompt yang sama persis memakan 75 detik. Nilai 20 detik yang semula
         * dipakai di sini akan selalu gagal.
         *
         * thinkingBudget sudah disetel 0 di requestBody(), sehingga panggilan seharusnya
         * jauh lebih cepat dari 75 detik itu. Batas longgar ini tetap dipertahankan sebagai
         * jaring pengaman: kalau suatu saat setelan itu diabaikan model, permintaan hanya
         * melambat, bukan gagal. Memendekkan timeout hanya mengubah lambat menjadi gagal.
         *
         * GeminiLiveTest mengukur dan melaporkan lamanya, jadi angka sebenarnya akan
         * terlihat begitu dijalankan dengan kunci yang masih berlaku.
         */
        const val TIMEOUT_MS = 120_000

        val DATE_PATTERN = Regex("""\d{4}-\d{2}-\d{2}""")
        val TIME_PATTERN = Regex("""\d{2}:\d{2}""")

        fun List<Any>.toJsonArray() = org.json.JSONArray(this)
    }
}

package com.bee.calenvoice

import com.bee.calenvoice.data.GeminiInferenceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Calendar

/**
 * Menguji Fitur 2 ke Gemini yang sebenarnya, langsung dari laptop. Tidak butuh HP
 * maupun emulator: ini unit test JVM biasa yang kebetulan memakai jaringan.
 *
 * Cara memakai:
 * 1. Isi GEMINI_API_KEY di local.properties dengan kunci dari aistudio.google.com.
 *    Bentuknya diawali "AIza".
 * 2. ./gradlew testDebugUnitTest
 *
 * Kalau kuncinya belum diisi atau bentuknya bukan API key, test ini melewati dirinya
 * sendiri lewat assumeTrue, sehingga hasil test keseluruhan tetap hijau dan tidak
 * menutupi kegagalan lain. Lihat pesan "assumption failed" di laporan untuk tahu
 * test ini dilewati, bukan lulus diam-diam.
 */
class GeminiLiveTest {

    @Test
    fun `Gemini mengubah kalimat Indonesia menjadi judul tanggal dan jam`() = runBlocking {
        val key = BuildConfig.GEMINI_API_KEY

        // Dilewati hanya kalau kuncinya belum diisi sama sekali. Jangan menambah syarat
        // soal bentuk kunci: token "AQ." sempat dikira tidak berlaku, padahal berfungsi,
        // hanya batas lajunya sangat ketat sehingga permintaan beruntun membuatnya
        // dijawab 404 berbadan kosong.
        assumeTrue(
            "GEMINI_API_KEY belum diisi di local.properties, test dilewati",
            key.isNotBlank(),
        )

        val repo = GeminiInferenceRepository(apiKey = key)

        val mulai = System.currentTimeMillis()
        val hasil = repo.extract("ingetin aku meeting jam 3 besok", System.currentTimeMillis())
        val detik = (System.currentTimeMillis() - mulai) / 1000.0

        val reminder = hasil.getOrElse {
            throw AssertionError("Panggilan ke Gemini gagal: ${it.message}", it)
        }

        println("Gemini menjawab dalam %.1f detik: %s".format(detik, reminder))
        if (detik > 15) {
            println(
                "CATATAN: $detik detik terasa lama untuk aplikasi suara. Pertimbangkan " +
                        "thinkingBudget = 0 atau model gemini-flash-lite-latest."
            )
        }

        assertTrue("Judul tidak boleh kosong", reminder.title.isNotBlank())
        assertTrue(
            "Tanggal harus yyyy-MM-dd, dapatnya '${reminder.date}'",
            Regex("""\d{4}-\d{2}-\d{2}""").matches(reminder.date),
        )
        assertTrue(
            "Jam harus HH:mm, dapatnya '${reminder.time}'",
            Regex("""\d{2}:\d{2}""").matches(reminder.time),
        )

        // "besok jam 3" harus jatuh pada 15:00, bukan 03:00, dan bukan hari ini.
        assertTrue("\"jam 3\" sore seharusnya 15:00, dapatnya ${reminder.time}", reminder.time == "15:00")
        assertTrue("\"besok\" seharusnya bukan hari ini", reminder.date != hariIni())
    }

    private fun hariIni(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }
}

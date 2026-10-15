package com.bee.calenvoice

import com.bee.calenvoice.model.Transcript
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptTest {
    private fun t(text: String) = Transcript(text, confidence = 1f)

    @Test
    fun `teks kosong terdeteksi`() { // FR-05
        assertTrue(t("   ").isEmpty())
        assertFalse(t("rapat").isEmpty())
    }

    // ------------------------------------------------------------------
    // FR-06: jam wajib disebut lengkap dengan angkanya.
    // Sistem tidak boleh menebak jam default, sesuai SRS bagian 1.2 dan 1.3.
    // ------------------------------------------------------------------

    @Test
    fun `jam beserta angkanya diterima`() {
        assertTrue(t("Rapat besok jam 9").hasTimeWithNumber())
        assertTrue(t("Meeting Pukul 14:00").hasTimeWithNumber())
        assertTrue(t("ingetin aku meeting jam 3 besok").hasTimeWithNumber())
        assertTrue("angka boleh disela satu kata", t("rapat jam setengah 8").hasTimeWithNumber())
    }

    @Test
    fun `tanpa kata waktu sama sekali ditolak`() {
        assertFalse(t("Rapat besok pagi").hasTimeWithNumber())
        assertFalse(t("ingetin meeting besok").hasTimeWithNumber())
    }

    @Test
    fun `kata waktu tanpa angka ditolak`() {
        // Kata kunci saja belum menunjukkan pukul berapa.
        assertFalse(t("ingetin meeting jam").hasTimeWithNumber())
        assertFalse(t("rapat pukul").hasTimeWithNumber())
    }

    @Test
    fun `kata waktu yang disusul kata bukan angka ditolak`() {
        assertFalse(t("meeting jam sore").hasTimeWithNumber())
        assertFalse(t("rapat jam siang nanti").hasTimeWithNumber())
        assertFalse(t("meeting pukul berapa ya").hasTimeWithNumber())
    }

    @Test
    fun `angka yang letaknya jauh bukan keterangan waktu`() {
        // "5" di sini jumlah orang, bukan jam. Jarak lebih dari satu kata ditolak.
        assertFalse(t("meeting jam sore nanti 5 orang datang").hasTimeWithNumber())
    }

    @Test
    fun `kata jamur tidak dianggap kata waktu`() {
        assertFalse(t("Beli jamur 3 kilo").hasTimeWithNumber())
    }
}

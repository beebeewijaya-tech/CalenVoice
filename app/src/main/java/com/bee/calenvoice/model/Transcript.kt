package com.bee.calenvoice.model

/** Gambar 3.1 — hasil speech-to-text. Bukan String biasa karena confidence ikut menentukan UI. */
data class Transcript(
    val text: String,
    val confidence: Float,
) {
    fun isEmpty() = text.isBlank() // FR-05

    /**
     * Memeriksa ucapan menyebut jam beserta angkanya (FR-06).
     *
     * Aturannya diperketat dari sekadar mencari kata "jam" atau "pukul", mengikuti
     * SRS bagian 1.2 dan 1.3: waktu yang belum lengkap harus menghasilkan pesan error
     * supaya pengguna merekam ulang, bukan ditebak oleh sistem. Kata kunci tanpa angka
     * belum menunjukkan pukul berapa, jadi belum cukup.
     *
     * Diterima:
     *   "meeting jam 3 besok", "rapat pukul 14:00", "jam setengah 8"
     *
     * Ditolak:
     *   "ingetin meeting besok"  tidak ada kata waktu sama sekali
     *   "ingetin meeting jam"    ada kata waktu, tidak ada angkanya
     *   "meeting jam sore"       ada kata waktu, yang menyusul bukan angka
     *   "beli jamur"             "jamur" bukan "jam", dijaga batas kata \b
     *
     * Satu kata boleh menyela sebelum angkanya, supaya bentuk sehari-hari seperti
     * "jam setengah 8" tetap lolos. Lebih dari itu ditolak, karena angka yang letaknya
     * jauh biasanya bukan keterangan waktu, misalnya "jam sore nanti 5 orang datang".
     */
    fun hasTimeWithNumber() = TIME_WITH_NUMBER.containsMatchIn(text) // FR-06

    private companion object {
        val TIME_WITH_NUMBER = Regex(
            """\b(jam|pukul)\b\s+(?:\S+\s+)?\d""",
            RegexOption.IGNORE_CASE,
        )
    }
}

/** Gambar 3.1 / L.1 — lima status layar perekaman. */
enum class RecordingState { IDLE, RECORDING, PROCESSING, DONE, ERROR }

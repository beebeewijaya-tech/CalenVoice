package com.bee.calenvoice.model

/** Gambar 3.1 — hasil speech-to-text. Bukan String biasa karena confidence ikut menentukan UI. */
data class Transcript(
    val text: String,
    val confidence: Float,
) {
    fun isEmpty() = text.isBlank()                       // FR-05
    fun hasTimeKeyword() = TIME_KEYWORD.containsMatchIn(text) // FR-06

    private companion object {
        val TIME_KEYWORD = Regex("""\b(jam|pukul)\b""", RegexOption.IGNORE_CASE)
    }
}

/** Gambar 3.1 / L.1 — lima status layar perekaman. */
enum class RecordingState { IDLE, RECORDING, PROCESSING, DONE, ERROR }

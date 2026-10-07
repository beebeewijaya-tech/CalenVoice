package com.bee.calenvoice.data

import com.bee.calenvoice.model.Transcript

/** Lapisan 2 — Fitur 1. Abstraksi agar ViewModel bisa diuji tanpa perangkat (Tabel 2.4). */
interface SpeechRepository {
    suspend fun listen(maxSeconds: Int = 60): Result<Transcript>
}

class AndroidSpeechRepository : SpeechRepository {
    // TODO: android.speech.SpeechRecognizer
    override suspend fun listen(maxSeconds: Int): Result<Transcript> = TODO()
}

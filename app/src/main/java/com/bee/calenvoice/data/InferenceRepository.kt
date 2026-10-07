package com.bee.calenvoice.data

import com.bee.calenvoice.model.Reminder

/** Lapisan 2 — Fitur 2. Satu-satunya jalan keluar ke internet (Client-Server, Gambar 2.3). */
interface InferenceRepository {
    /** [now] disertakan agar Gemini bisa menghitung kata relatif seperti "besok" (FR-08). */
    suspend fun extract(text: String, now: Long): Result<Reminder>
}

class GeminiInferenceRepository : InferenceRepository {
    // TODO: HTTPS ke Gemini API, parse JSON {title, date, time} (FR-09)
    override suspend fun extract(text: String, now: Long): Result<Reminder> = TODO()
}

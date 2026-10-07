package com.bee.calenvoice.model

/** Gambar 3.2/3.3 — hasil ekstraksi Gemini. Hidup di memori saja, tidak ada DB lokal (Bab 5). */
data class Reminder(
    val title: String,
    val date: String, // yyyy-MM-dd
    val time: String, // HH:mm
)

/** Gambar 5.2 — satu baris tabel Events milik Android Calendar Provider. */
data class CalendarEvent(
    val eventId: Long? = null,   // Events._ID, diisi provider
    val calendarId: Long,        // Events.CALENDAR_ID
    val title: String,           // Events.TITLE
    val dtStart: Long,           // epoch millis UTC
    val dtEnd: Long,             // dihitung aplikasi
    val timeZone: String,        // Events.EVENT_TIMEZONE
)

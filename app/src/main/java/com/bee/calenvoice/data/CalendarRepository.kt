package com.bee.calenvoice.data

import com.bee.calenvoice.model.Reminder

/** Lapisan 2 — Fitur 3. Satu-satunya operasi tulis data di seluruh sistem (Gambar 4.3). */
interface CalendarRepository {
    fun hasCalendarPermission(): Boolean

    /** @return Events._ID dari Calendar Provider, bukan primary key aplikasi (Tabel 6.2). */
    suspend fun createEvent(reminder: Reminder): Result<Long>
}

class DeviceCalendarRepository : CalendarRepository {
    // TODO: android.provider.CalendarContract.Events insert (Tabel 5.2)
    override fun hasCalendarPermission(): Boolean = TODO()
    override suspend fun createEvent(reminder: Reminder): Result<Long> = TODO()
}

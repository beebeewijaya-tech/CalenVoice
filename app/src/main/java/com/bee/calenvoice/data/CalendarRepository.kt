package com.bee.calenvoice.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.bee.calenvoice.model.CalendarEvent
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.util.Log

/**
 * Lapisan 2 (Data Layer), Fitur 3: Save Calendar.
 *
 * Ini satu-satunya operasi tulis data di seluruh sistem (Gambar 4.3). Aplikasi tidak
 * punya database lokal (Bab 5), jadi Reminder hasil Gemini langsung ditulis ke
 * Android Calendar Provider.
 *
 * Catatan arsitektur (Layered + MVVM):
 * Repository tidak boleh menampilkan dialog izin karena itu urusan UI. Karena itu
 * requestCalendarPermission() dari Gambar 3.3 dijalankan di lapisan UI (lihat
 * ReminderResultRoute), sedangkan repository hanya menyediakan pengecekan izin
 * lewat hasCalendarPermission().
 */
interface CalendarRepository {

    /** @return true jika READ_CALENDAR dan WRITE_CALENDAR sudah diberikan pengguna. */
    fun hasCalendarPermission(): Boolean

    /**
     * Membuat satu event di kalender HP dari [reminder].
     *
     * @return Result sukses berisi Events._ID dari Calendar Provider (bukan primary key
     * milik aplikasi, karena aplikasi tidak punya database; lihat Tabel 5.2). Result gagal
     * berisi salah satu dari [CalendarPermissionException], [NoWritableCalendarException],
     * [InvalidReminderException], atau exception lain dari sistem.
     */
    suspend fun createEvent(reminder: Reminder): Result<Long>
}

/** Izin kalender belum diberikan atau dicabut. Memicu Alur Alternatif D. */
class CalendarPermissionException : Exception("Izin kalender belum diberikan")

/** Tidak ada kalender yang bisa ditulis di HP (misalnya belum ada akun Google). */
class NoWritableCalendarException : Exception("Tidak ada kalender yang bisa dipakai")

/** Tanggal atau jam pada Reminder tidak bisa dibaca (format bukan yyyy-MM-dd dan HH:mm). */
class InvalidReminderException(message: String) : Exception(message)

/**
 * Implementasi CalendarRepository memakai ContentResolver + CalendarContract.
 *
 * @param context sebaiknya applicationContext supaya tidak bocor memori (leak) dari Activity.
 */
class DeviceCalendarRepository(private val context: Context) : CalendarRepository {

    override fun hasCalendarPermission(): Boolean =
        isGranted(Manifest.permission.READ_CALENDAR) &&
                isGranted(Manifest.permission.WRITE_CALENDAR)

    override suspend fun createEvent(reminder: Reminder): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Cek izin lagi, karena pengguna bisa mencabutnya lewat Pengaturan kapan saja.
                if (!hasCalendarPermission()) {
                    return@withContext Result.failure(CalendarPermissionException())
                }

                // 2. Ubah date + time (String) menjadi epoch milidetik (Tabel 5.2, dtStart).
                val dtStart = parseToEpochMillis(reminder.date, reminder.time)
                    ?: return@withContext Result.failure(
                        InvalidReminderException("Format tanggal atau jam tidak valid")
                    )

                // 3. Cari kalender tujuan (Tabel 5.2, calendarId).
                val calendarId = findWritableCalendarId()
                    ?: return@withContext Result.failure(NoWritableCalendarException())

                // 4. Petakan Reminder ke CalendarEvent (Bab 5.3, Gambar 3.3).
                val event = CalendarEvent(
                    calendarId = calendarId,
                    title = reminder.title,
                    dtStart = dtStart,
                    dtEnd = dtStart + DEFAULT_DURATION_MS,
                    timeZone = TimeZone.getDefault().id,
                )

                // 5. Insert. URI hasilnya berakhir dengan Events._ID (FR-11).
                val uri = context.contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    event.toContentValues()
                ) ?: return@withContext Result.failure(Exception("Calendar Provider menolak event"))

                Result.success(ContentUris.parseId(uri))
            } catch (e: CancellationException) {
                throw e // jangan ditelan, supaya coroutine bisa dibatalkan dengan benar
            } catch (e: SecurityException) {
                // Izin dicabut tepat di tengah proses.
                Result.failure(CalendarPermissionException())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Pemetaan CalendarEvent ke kolom Events sesuai Tabel 5.2. eventId tidak diisi, karena dibuat provider. */
    private fun CalendarEvent.toContentValues() = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.DTSTART, dtStart)
        put(CalendarContract.Events.DTEND, dtEnd)
        put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
    }

    /**
     * Mencari ID kalender tujuan yang boleh ditulis (access level minimal CONTRIBUTOR).
     *
     * Urutan prioritas:
     * 1. Kalender akun Google milik sendiri (ACCOUNT_TYPE = com.google dan akun == owner).
     * 2. Kalender akun Google lain yang writable.
     * 3. Kalender lokal/non-Google (misalnya "PC Sync" bawaan Tecno) sebagai cadangan terakhir.
     *
     * IS_PRIMARY sengaja tidak dipakai, karena beberapa HP menandai kalender lokal sebagai
     * primary sehingga event tersimpan tapi tidak muncul di Google Calendar.
     * Filter VISIBLE juga tidak dipakai di query supaya semua kalender tercatat di Logcat.
     * Butuh READ_CALENDAR.
     *
     * Setiap kalender dan pilihan akhir dicatat ke Logcat (tag "CalenVoice").
     */
    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        // Sengaja TIDAK memfilter VISIBLE di query, supaya semua kalender muncul di log.
        val selection =
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"

        var googleOwnId: Long? = null   // kalender Google milik sendiri (account == owner)
        var googleAnyId: Long? = null   // kalender Google lain yang writable
        var localId: Long? = null       // cadangan terakhir

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val account = c.getString(1)
                val type = c.getString(2)
                val owner = c.getString(3)
                val visible = c.getInt(4) == 1
                Log.d(TAG, "Kalender id=$id tipe=$type visible=$visible")

                if (type == "com.google") {
                    if (account != null && account == owner && googleOwnId == null) googleOwnId = id
                    if (googleAnyId == null) googleAnyId = id
                } else if (localId == null) {
                    localId = id
                }
            }
        }

        val chosen = googleOwnId ?: googleAnyId ?: localId
        Log.d(TAG, "Dipilih kalender id=$chosen (google=${googleOwnId ?: googleAnyId})")
        return chosen
    }

    /**
     * Parsing "yyyy-MM-dd" + "HH:mm" memakai zona waktu perangkat.
     * isLenient = false supaya tanggal ngawur seperti 2026-02-31 ditolak, bukan dibulatkan.
     * SimpleDateFormat dipilih (bukan java.time) karena minSdk 24 dan java.time baru aman
     * mulai API 26 tanpa desugaring.
     * @return epoch milidetik, atau null jika format salah.
     */
    private fun parseToEpochMillis(date: String, time: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getDefault()
        }.parse("$date $time")?.time
    } catch (e: ParseException) {
        null
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        /** Penanda log di Logcat. Filter dengan "CalenVoice" untuk melihat jejak penyimpanan event. */
        const val TAG = "CalenVoice"

        /** Durasi default 1 jam (Tabel 5.2, dtEnd). Android mewajibkan DTEND atau DURATION. */
        const val DEFAULT_DURATION_MS = 3_600_000L
    }
}
package com.bee.calenvoice

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bee.calenvoice.data.DeviceCalendarRepository
import com.bee.calenvoice.model.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.TimeZone

/**
 * Membuktikan Fitur 3 benar-benar menulis ke Android Calendar Provider, bukan sekadar
 * tersambung di kode. Dijalankan di perangkat atau emulator:
 *
 *     ./gradlew connectedDebugAndroidTest
 *
 * Tidak menyentuh mikrofon maupun Gemini, jadi Fitur 3 bisa diuji berdiri sendiri
 * walaupun Fitur 1 dan 2 sedang terhalang.
 *
 * Emulator polos tidak punya kalender sama sekali, sehingga createEvent akan selalu
 * gagal dengan NoWritableCalendarException. Karena itu test ini membuat satu kalender
 * lokal lebih dulu sebagai persiapan, lalu menghapusnya lagi di akhir supaya perangkat
 * kembali seperti semula.
 */
@RunWith(AndroidJUnit4::class)
class CalendarWriteTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver

    private var calendarId: Long = -1
    private var eventId: Long = -1

    @Before
    fun siapkan() {
        // Izin diberikan lewat UiAutomation supaya test tidak butuh dependency tambahan.
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            .forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it) }

        calendarId = cariKalender() ?: buatKalenderLokal()
    }

    @After
    fun bersihkan() {
        if (eventId > 0) {
            resolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                null, null,
            )
        }
        // Hanya hapus kalender yang dibuat test ini sendiri.
        resolver.delete(
            uriSyncAdapter(CalendarContract.Calendars.CONTENT_URI),
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ?",
            arrayOf(AKUN_UJI),
        )
    }

    @Test
    fun reminder_tersimpan_sebagai_satu_baris_di_tabel_Events() = runBlocking {
        val repo = DeviceCalendarRepository(context)
        val besok = tanggal(hariKeDepan = 1)
        val reminder = Reminder(title = "Meeting uji", date = besok, time = "15:00")

        val hasil = repo.createEvent(reminder)

        eventId = hasil.getOrElse {
            throw AssertionError("createEvent gagal: ${it::class.simpleName}: ${it.message}", it)
        }
        assertTrue("Events._ID harus bernilai wajar, dapatnya $eventId", eventId > 0)

        // Dibaca kembali dari provider. Inilah bukti bahwa event sungguh ada di kalender,
        // bukan sekadar insert() yang mengembalikan URI.
        resolver.query(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.CALENDAR_ID,
            ),
            null, null, null,
        ).use { c ->
            assertNotNull("Event tidak ditemukan setelah disimpan", c)
            assertTrue("Event tidak ditemukan setelah disimpan", c!!.moveToFirst())

            assertEquals("Meeting uji", c.getString(0))

            val dtStart = c.getLong(1)
            val dtEnd = c.getLong(2)
            assertTrue("DTSTART harus terisi", dtStart > 0)
            assertTrue("DTEND harus sesudah DTSTART (Tabel 5.2)", dtEnd > dtStart)

            assertEquals(
                "EVENT_TIMEZONE wajib diisi bersama DTSTART (Tabel 5.2)",
                TimeZone.getDefault().id, c.getString(3),
            )
            assertEquals("CALENDAR_ID harus menunjuk kalender tujuan", calendarId, c.getLong(4))

            // DTSTART harus jatuh pada jam 15:00 waktu perangkat.
            val jam = Calendar.getInstance().apply { timeInMillis = dtStart }
            assertEquals("Jam mulai harus 15", 15, jam.get(Calendar.HOUR_OF_DAY))
            assertEquals("Menit mulai harus 0", 0, jam.get(Calendar.MINUTE))
        }
    }

    // ------------------------------------------------------------------
    // Persiapan kalender
    // ------------------------------------------------------------------

    private fun cariKalender(): Long? = resolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(CalendarContract.Calendars._ID),
        "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
        arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
        null,
    ).use { if (it != null && it.moveToFirst()) it.getLong(0) else null }

    private fun buatKalenderLokal(): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, AKUN_UJI)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, AKUN_UJI)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Kalender Uji CalenVoice")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF800020.toInt())
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER,
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, AKUN_UJI)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }
        val uri = resolver.insert(uriSyncAdapter(CalendarContract.Calendars.CONTENT_URI), values)
            ?: error("Tidak bisa membuat kalender lokal untuk pengujian")
        return ContentUris.parseId(uri)
    }

    /** Membuat kalender hanya boleh lewat URI sync adapter, bukan URI biasa. */
    private fun uriSyncAdapter(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, AKUN_UJI)
        .appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL,
        )
        .build()

    private fun tanggal(hariKeDepan: Int): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, hariKeDepan) }
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }

    private companion object {
        const val AKUN_UJI = "CalenVoiceUji"
    }
}

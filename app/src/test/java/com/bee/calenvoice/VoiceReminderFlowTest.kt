package com.bee.calenvoice

import com.bee.calenvoice.data.CalendarRepository
import com.bee.calenvoice.data.InferenceRepository
import com.bee.calenvoice.data.SpeechRepository
import com.bee.calenvoice.model.RecordingState
import com.bee.calenvoice.model.Reminder
import com.bee.calenvoice.model.Transcript
import com.bee.calenvoice.ui.VoiceReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Menguji alur Fitur 1 ke Fitur 2 di VoiceReminderViewModel tanpa perangkat Android.
 *
 * Inilah yang dijanjikan Layered Architecture pada Tabel 2.4: ketiga repository berbentuk
 * interface, jadi saat unit test bisa diganti versi palsu. Tidak ada mikrofon, tidak ada
 * panggilan ke Gemini, dan tidak ada kalender yang disentuh di sini.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceReminderFlowTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun pasangMainDispatcher() = Dispatchers.setMain(dispatcher)
    @After fun lepasMainDispatcher() = Dispatchers.resetMain()

    // ------------------------------------------------------------------
    // Repository palsu
    // ------------------------------------------------------------------

    private class FakeSpeech(
        private val hasil: Result<Transcript>,
        private val izin: Boolean = true,
    ) : SpeechRepository {
        var dipanggil = false
        override fun hasMicPermission() = izin
        override fun isAvailable() = true
        override suspend fun listen(maxSeconds: Int): Result<Transcript> {
            dipanggil = true
            return hasil
        }
        override fun stopListening() = Unit
    }

    private class FakeInference(
        private val hasil: Result<Reminder>,
    ) : InferenceRepository {
        var dipanggil = false
        var teksYangDikirim: String? = null
        override suspend fun extract(text: String, now: Long): Result<Reminder> {
            dipanggil = true
            teksYangDikirim = text
            return hasil
        }
    }

    private class FakeCalendar : CalendarRepository {
        override fun hasCalendarPermission() = true
        override suspend fun createEvent(reminder: Reminder) = Result.success(1L)
    }

    private fun viewModel(
        speech: SpeechRepository,
        inference: InferenceRepository = FakeInference(Result.success(CONTOH)),
    ) = VoiceReminderViewModel(speech, inference, FakeCalendar())

    // ------------------------------------------------------------------
    // Kasus uji
    // ------------------------------------------------------------------

    @Test
    fun `izin mikrofon belum ada maka minta dialog dan tidak merekam`() = runTest {
        val speech = FakeSpeech(Result.success(Transcript("apa pun", 1f)), izin = false)
        val vm = viewModel(speech)

        vm.onRecordClicked()
        advanceUntilIdle()

        assertTrue("View harus diminta memunculkan dialog izin", vm.uiState.value.needsMicPermission)
        assertFalse("Perekaman tidak boleh dimulai sebelum izin ada", speech.dipanggil)
    }

    @Test
    fun `ucapan tanpa kata waktu ditolak dan Gemini tidak dipanggil`() = runTest {
        // FR-06 dan UC-05 Alur Alternatif A.
        val inference = FakeInference(Result.success(CONTOH))
        val vm = viewModel(FakeSpeech(Result.success(Transcript("ingetin meeting besok", 1f))), inference)

        vm.onRecordClicked()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(RecordingState.ERROR, state.recording)
        assertTrue(state.error!!.contains("jam"))
        assertFalse("Gemini tidak boleh dipanggil kalau validasi gagal", inference.dipanggil)
        assertNull(state.reminder)
    }

    @Test
    fun `suara kosong ditolak sebelum sampai ke Gemini`() = runTest {
        // FR-05.
        val inference = FakeInference(Result.success(CONTOH))
        val vm = viewModel(FakeSpeech(Result.success(Transcript("   ", 1f))), inference)

        vm.onRecordClicked()
        advanceUntilIdle()

        assertEquals(RecordingState.ERROR, vm.uiState.value.recording)
        assertFalse(inference.dipanggil)
    }

    @Test
    fun `ucapan lolos validasi diteruskan ke Gemini dan menghasilkan reminder`() = runTest {
        val teks = "ingetin aku meeting jam 3 besok"
        val inference = FakeInference(Result.success(CONTOH))
        val vm = viewModel(FakeSpeech(Result.success(Transcript(teks, 0.9f))), inference)

        vm.onRecordClicked()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("Teks transkrip harus diteruskan apa adanya", inference.dipanggil)
        assertEquals(teks, inference.teksYangDikirim)
        assertEquals(RecordingState.DONE, state.recording)
        assertNotNull(state.reminder)
        assertEquals("Meeting", state.reminder!!.title)
        assertNull(state.error)
    }

    private companion object {
        val CONTOH = Reminder(title = "Meeting", date = "2026-10-15", time = "15:00")
    }
}

package com.bee.calenvoice

import com.bee.calenvoice.model.Transcript
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptTest {
    private fun t(text: String) = Transcript(text, confidence = 1f)

    @Test fun `teks kosong terdeteksi`() {            // FR-05
        assertTrue(t("   ").isEmpty())
        assertFalse(t("rapat").isEmpty())
    }

    @Test fun `kata waktu terdeteksi`() {             // FR-06
        assertTrue(t("Rapat besok jam 9").hasTimeKeyword())
        assertTrue(t("Meeting Pukul 14:00").hasTimeKeyword())
        assertFalse(t("Rapat besok pagi").hasTimeKeyword())
        assertFalse(t("Beli jamur").hasTimeKeyword())  // "jam" bukan kata utuh
    }
}

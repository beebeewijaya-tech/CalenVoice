package com.bee.calenvoice

import com.bee.calenvoice.data.GeminiBusyException
import com.bee.calenvoice.data.GeminiException
import com.bee.calenvoice.data.ulangiSaatSibuk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji aturan pengulangan saat Gemini sibuk, tanpa menyentuh jaringan.
 *
 * Yang dijaga di sini adalah jenis bug yang gampang lolos dari pembacaan mata:
 * salah hitung jumlah percobaan, lupa melempar ulang setelah percobaan habis,
 * dan ikut mengulang kegagalan yang tidak akan pernah membaik.
 *
 * runTest memajukan waktu virtual, jadi delay di dalamnya tidak membuat test lambat.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UlangiSaatSibukTest {

    @Test
    fun `berhasil di percobaan pertama maka tidak ada pengulangan`() = runTest {
        var dipanggil = 0
        val hasil = ulangiSaatSibuk(maksPercobaan = 3, jedaMs = 1_000) {
            dipanggil++
            "oke"
        }
        assertEquals("oke", hasil)
        assertEquals(1, dipanggil)
    }

    @Test
    fun `sibuk dua kali lalu berhasil maka hasilnya dikembalikan`() = runTest {
        var dipanggil = 0
        val hasil = ulangiSaatSibuk(maksPercobaan = 3, jedaMs = 1_000) {
            dipanggil++
            if (dipanggil < 3) throw GeminiBusyException("sedang ramai")
            "akhirnya berhasil"
        }
        assertEquals("akhirnya berhasil", hasil)
        assertEquals(3, dipanggil)
    }

    @Test
    fun `sibuk terus maka berhenti tepat di batas dan melempar kegagalan terakhir`() = runTest {
        var dipanggil = 0
        val e = runCatching {
            ulangiSaatSibuk(maksPercobaan = 3, jedaMs = 1_000) {
                dipanggil++
                throw GeminiBusyException("ramai ke-$dipanggil")
            }
        }.exceptionOrNull()

        assertEquals("Tidak boleh mencoba lebih dari batas", 3, dipanggil)
        assertTrue(e is GeminiBusyException)
        assertEquals("Yang dilempar harus kegagalan terakhir", "ramai ke-3", e!!.message)
    }

    @Test
    fun `kegagalan yang tidak sementara tidak diulang`() = runTest {
        // Kunci salah atau model tidak ada tidak akan membaik walau dicoba lagi,
        // jadi harus langsung dilempar supaya pengguna cepat tahu.
        var dipanggil = 0
        val e = runCatching {
            ulangiSaatSibuk(maksPercobaan = 3, jedaMs = 1_000) {
                dipanggil++
                throw GeminiException("kunci ditolak")
            }
        }.exceptionOrNull()

        assertEquals("Hanya boleh dicoba sekali", 1, dipanggil)
        assertTrue(e is GeminiException)
        assertTrue(e !is GeminiBusyException)
    }
}

package id.sch.mts241alikhlash.istiqra.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

class XorCipher : TokenCipher {
    var encryptedPlain: String? = null

    override fun encrypt(plain: ByteArray): ByteArray {
        encryptedPlain = String(plain)
        return plain.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    }

    override fun decrypt(cipher: ByteArray): ByteArray = encrypt(cipher)
}

@RunWith(RobolectricTestRunner::class)
class KeystoreTokenStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val auth =
        StoredAuth(
            tokens = AuthTokens("access-secret", "refresh-secret-value", 42L),
            user = AuthUser("u1", "Ahmad", setOf("STUDENT")),
        )

    @Test
    fun roundtrip_returns_saved_auth() =
        runTest {
            val store = KeystoreTokenStore(context, XorCipher())
            store.save(auth)

            assertEquals(auth, store.load())

            store.clear()
            assertNull(store.load())
        }

    @Test
    fun stored_bytes_do_not_contain_refresh_token_plaintext() =
        runTest {
            val cipher = XorCipher()
            KeystoreTokenStore(context, cipher).save(auth)

            assertTrue(cipher.encryptedPlain!!.contains("refresh-secret-value"))
            val files = File(context.filesDir, "datastore").listFiles().orEmpty()
            val raw = files.joinToString("") { String(it.readBytes(), Charsets.ISO_8859_1) }

            assertFalse(raw.contains("refresh-secret-value"))
            assertFalse(raw.contains("access-secret"))
        }
}

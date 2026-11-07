package id.sch.mts241alikhlash.exam.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.authDataStore by preferencesDataStore(name = "auth_tokens")

class KeystoreTokenStore(
    private val context: Context,
    private val cipher: TokenCipher = AndroidKeystoreCipher(),
) : TokenStore {
    override suspend fun load(): StoredAuth? {
        val encoded = context.authDataStore.data.first()[AUTH] ?: return null
        return runCatching {
            val plain = cipher.decrypt(Base64.decode(encoded, Base64.NO_WRAP))
            json.decodeFromString<StoredAuth>(String(plain, Charsets.UTF_8))
        }.getOrNull()
    }

    override suspend fun save(auth: StoredAuth) {
        val plain = json.encodeToString(StoredAuth.serializer(), auth).toByteArray(Charsets.UTF_8)
        val encoded = Base64.encodeToString(cipher.encrypt(plain), Base64.NO_WRAP)
        context.authDataStore.edit { it[AUTH] = encoded }
    }

    override suspend fun clear() {
        context.authDataStore.edit { it.remove(AUTH) }
    }

    private companion object {
        val AUTH = stringPreferencesKey("auth")
        val json = Json { ignoreUnknownKeys = true }
    }
}

class AndroidKeystoreCipher : TokenCipher {
    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(cipher: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, cipher, 0, IV_BYTES))
        return c.doFinal(cipher, IV_BYTES, cipher.size - IV_BYTES)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec
                .Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "exam_auth_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

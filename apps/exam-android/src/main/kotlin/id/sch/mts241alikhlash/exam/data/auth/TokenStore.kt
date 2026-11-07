package id.sch.mts241alikhlash.exam.data.auth

interface TokenStore {
    suspend fun load(): StoredAuth?

    suspend fun save(auth: StoredAuth)

    suspend fun clear()
}

interface TokenCipher {
    fun encrypt(plain: ByteArray): ByteArray

    fun decrypt(cipher: ByteArray): ByteArray
}

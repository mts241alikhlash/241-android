package id.sch.mts241alikhlash.exam.data.auth

import id.sch.mts241alikhlash.exam.api.identity.apis.NativeAuthenticationApi
import id.sch.mts241alikhlash.exam.api.identity.models.NativeLoginDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeLogoutDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeRefreshDto
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.remote.guarded
import id.sch.mts241alikhlash.exam.data.remote.unwrapData
import id.sch.mts241alikhlash.exam.data.remote.unwrapUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepository(
    private val api: NativeAuthenticationApi,
    private val store: TokenStore,
    private val deviceId: () -> String,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val refreshLock = Mutex()
    private val _rejected = MutableStateFlow(false)
    val rejected: StateFlow<Boolean> = _rejected.asStateFlow()

    suspend fun login(
        identifier: String,
        password: String,
        requiredRole: String,
    ): AuthUser {
        val response =
            guarded {
                api
                    .nativeAuthControllerLogin(
                        NativeLoginDto(identifier.trim(), password, deviceId()),
                    ).unwrapData { it.data }
            }
        val firstTokens = response.toTokens(now())
        val user = response.user.toUser()
        if (requiredRole !in user.roles) {
            revoke(firstTokens.refreshToken)
            throw ApiException(403, "Akun ini tidak dapat masuk di sini.")
        }
        store.save(StoredAuth(firstTokens, user))
        _rejected.value = false
        return user
    }

    suspend fun relogin(
        identifier: String,
        password: String,
    ) {
        val stored = store.load()
        val response =
            guarded {
                api
                    .nativeAuthControllerLogin(
                        NativeLoginDto(identifier.trim(), password, deviceId()),
                    ).unwrapData { it.data }
            }
        val reloginTokens = response.toTokens(now())
        val user = response.user.toUser()
        if (stored != null && user.id != stored.user.id) {
            revoke(reloginTokens.refreshToken)
            throw ApiException(403, "Akun berbeda dengan akun ujian.")
        }
        if (ROLE_STUDENT !in user.roles) {
            revoke(reloginTokens.refreshToken)
            throw ApiException(403, "Akun ini tidak dapat masuk di sini.")
        }
        store.save(StoredAuth(reloginTokens, user))
        _rejected.value = false
    }

    suspend fun current(): StoredAuth? = store.load()

    suspend fun currentStudent(): AuthUser? = store.load()?.user?.takeIf { it.isStudent }

    private suspend fun revoke(refreshToken: String) {
        runCatching { api.nativeAuthControllerLogout(NativeLogoutDto(refreshToken)).unwrapUnit() }
    }

    suspend fun refresh(
        clearOnRejected: Boolean = true,
        staleAccessToken: String? = null,
    ): AuthTokens? {
        val before = store.load()?.tokens?.refreshToken ?: return null
        if (_rejected.value) return null
        return refreshLock.withLock {
            val stored = store.load() ?: return null
            if (stored.tokens.refreshToken != before) return stored.tokens
            if (staleAccessToken != null && stored.tokens.accessToken != staleAccessToken) {
                return stored.tokens
            }
            try {
                val response =
                    guarded {
                        api
                            .nativeAuthControllerRefresh(
                                NativeRefreshDto(before, deviceId()),
                            ).unwrapData { it.data }
                    }
                val tokens = response.toTokens(now(), before)
                store.save(stored.copy(tokens = tokens))
                tokens
            } catch (e: ApiException) {
                if (e.statusCode == 401 || e.statusCode == 403) {
                    if (clearOnRejected) store.clear() else _rejected.value = true
                }
                null
            }
        }
    }

    suspend fun logout() {
        val refreshToken = store.load()?.tokens?.refreshToken
        if (refreshToken != null) {
            revoke(refreshToken)
        }
        store.clear()
    }
}

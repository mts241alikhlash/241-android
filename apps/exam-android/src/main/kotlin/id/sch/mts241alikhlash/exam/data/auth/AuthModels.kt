package id.sch.mts241alikhlash.exam.data.auth

import id.sch.mts241alikhlash.exam.api.identity.models.NativeAuthResponseDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeUserDto
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.remote.GENERIC_ERROR_MESSAGE
import kotlinx.serialization.Serializable

const val ROLE_STUDENT = "STUDENT"
const val ROLE_TEACHER = "TEACHER"

@Serializable
data class AuthUser(
    val id: String,
    val name: String,
    val roles: Set<String>,
) {
    val isStudent: Boolean get() = ROLE_STUDENT in roles
    val isTeacher: Boolean get() = ROLE_TEACHER in roles
}

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
)

@Serializable
data class StoredAuth(
    val tokens: AuthTokens,
    val user: AuthUser,
)

internal fun NativeUserDto.toUser() = AuthUser(id, name, roles.toSet())

internal fun NativeAuthResponseDto.toTokens(
    now: Long,
    held: String? = null,
) = AuthTokens(
    accessToken,
    refreshToken ?: held ?: throw ApiException(0, GENERIC_ERROR_MESSAGE),
    now + expiresIn * 1000L,
)

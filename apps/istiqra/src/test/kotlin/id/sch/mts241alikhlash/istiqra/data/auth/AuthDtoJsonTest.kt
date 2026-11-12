package id.sch.mts241alikhlash.istiqra.data.auth

import id.sch.mts241alikhlash.istiqra.api.identity.models.NativeAuthResponseDto
import id.sch.mts241alikhlash.istiqra.api.identity.models.NativeLoginDto
import id.sch.mts241alikhlash.istiqra.api.identity.models.NativeLogoutDto
import id.sch.mts241alikhlash.istiqra.api.identity.models.NativeRefreshDto
import id.sch.mts241alikhlash.istiqra.api.identity.models.NativeUserDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDtoJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun login_response_reads_the_camel_case_wire_shape() {
        val dto =
            json.decodeFromString<NativeAuthResponseDto>(
                """{"accessToken":"a","refreshToken":"r","expiresIn":900,"user":{"id":"u1","name":"Ahmad Fauzi","roles":["STUDENT"]}}""",
            )
        assertEquals(NativeAuthResponseDto("a", "r", 900L, NativeUserDto("u1", "Ahmad Fauzi", listOf("STUDENT"))), dto)
    }

    @Test
    fun a_raced_refresh_has_a_null_refresh_token() {
        val dto =
            json.decodeFromString<NativeAuthResponseDto>(
                """{"accessToken":"a","refreshToken":null,"expiresIn":900,"user":{"id":"u1","name":"x","roles":[]}}""",
            )
        assertNull(dto.refreshToken)
    }

    @Test
    fun requests_serialize_camel_case() {
        assertEquals(
            """{"identifier":"2024001","password":"p","deviceId":"d"}""",
            json.encodeToString(NativeLoginDto.serializer(), NativeLoginDto("2024001", "p", "d")),
        )
        assertEquals(
            """{"refreshToken":"r","deviceId":"d"}""",
            json.encodeToString(NativeRefreshDto.serializer(), NativeRefreshDto("r", "d")),
        )
        assertEquals(
            """{"refreshToken":"r"}""",
            json.encodeToString(NativeLogoutDto.serializer(), NativeLogoutDto("r")),
        )
    }
}

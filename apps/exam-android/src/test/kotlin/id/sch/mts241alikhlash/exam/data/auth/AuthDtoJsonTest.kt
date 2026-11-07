package id.sch.mts241alikhlash.exam.data.auth

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDtoJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun login_response_reads_the_camel_case_wire_shape() {
        val dto =
            json.decodeFromString<AuthResponseDto>(
                """{"accessToken":"a","refreshToken":"r","expiresIn":900,"user":{"id":"u1","name":"Ahmad Fauzi","roles":["STUDENT"]}}""",
            )
        assertEquals(AuthResponseDto("a", "r", 900, AuthUserDto("u1", "Ahmad Fauzi", listOf("STUDENT"))), dto)
    }

    @Test
    fun a_raced_refresh_has_a_null_refresh_token() {
        val dto =
            json.decodeFromString<AuthResponseDto>(
                """{"accessToken":"a","refreshToken":null,"expiresIn":900,"user":{"id":"u1","name":"x","roles":[]}}""",
            )
        assertNull(dto.refreshToken)
    }

    @Test
    fun requests_serialize_camel_case() {
        assertEquals(
            """{"identifier":"2024001","password":"p","deviceId":"d"}""",
            json.encodeToString(LoginRequest.serializer(), LoginRequest("2024001", "p", "d")),
        )
        assertEquals(
            """{"refreshToken":"r","deviceId":"d"}""",
            json.encodeToString(RefreshRequest.serializer(), RefreshRequest("r", "d")),
        )
        assertEquals(
            """{"refreshToken":"r"}""",
            json.encodeToString(LogoutRequest.serializer(), LogoutRequest("r")),
        )
    }
}

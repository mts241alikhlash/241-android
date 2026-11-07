package id.sch.mts241alikhlash.exam.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class ApiEnvelopeTest {
    @Serializable
    data class Item(
        val name: String,
    )

    private interface Api {
        @GET("item")
        suspend fun item(): ApiEnvelope<Item>
    }

    private lateinit var server: MockWebServer
    private lateinit var api: Api

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(
                    Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()),
                ).build()
                .create(Api::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun unwrap_returns_data_when_present() {
        assertEquals(Item("a"), ApiEnvelope(200, "OK", Item("a")).unwrap())
    }

    @Test
    fun unwrap_throws_ApiException_with_server_message_when_data_null() {
        val error =
            assertThrows(ApiException::class.java) {
                ApiEnvelope<Item>(409, "Sesi aktif di perangkat lain", null).unwrap()
            }
        assertEquals(409, error.statusCode)
        assertEquals("Sesi aktif di perangkat lain", error.message)
    }

    @Test
    fun apiCall_returns_unwrapped_data_on_success() =
        runTest {
            server.enqueue(
                MockResponse().setBody("""{"statusCode":200,"message":"OK","data":{"name":"x"}}"""),
            )
            assertEquals(Item("x"), apiCall { api.item() })
        }

    @Test
    fun apiCall_maps_http_401_body_to_ApiException_401() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"statusCode":401,"message":"Kredensial salah","data":null}"""),
            )
            val error = runCatching { apiCall { api.item() } }.exceptionOrNull() as ApiException
            assertEquals(401, error.statusCode)
            assertEquals("Kredensial salah", error.message)
        }

    @Test
    fun apiCall_maps_non_json_500_to_generic_message() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500).setBody("<html>boom</html>"))
            val error = runCatching { apiCall { api.item() } }.exceptionOrNull() as ApiException
            assertEquals(500, error.statusCode)
            assertEquals("Terjadi kesalahan. Coba lagi.", error.message)
        }

    @Test
    fun apiCall_maps_IOException_to_status_0() =
        runTest {
            server.shutdown()
            val error = runCatching { apiCall { api.item() } }.exceptionOrNull() as ApiException
            assertEquals(0, error.statusCode)
            assertEquals("Tidak dapat terhubung ke server.", error.message)
        }

    @Test
    fun userMessage_keeps_ApiException_message() {
        assertEquals("Sesi aktif", ApiException(409, "Sesi aktif").userMessage())
    }

    @Test
    fun userMessage_hides_internal_exception_details() {
        assertEquals(GENERIC_ERROR_MESSAGE, IllegalStateException("Keystore internal").userMessage())
    }
}

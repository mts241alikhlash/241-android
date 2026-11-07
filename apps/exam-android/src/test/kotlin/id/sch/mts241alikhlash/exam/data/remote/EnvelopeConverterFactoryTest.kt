package id.sch.mts241alikhlash.exam.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class EnvelopeConverterFactoryTest {
    @Serializable
    data class Item(
        val name: String,
    )

    private interface Api {
        @GET("list")
        suspend fun list(): List<Item>

        @GET("item")
        suspend fun item(): Item

        @GET("wrapped")
        suspend fun wrapped(): ApiEnvelope<Item>
    }

    private lateinit var server: MockWebServer
    private lateinit var api: Api

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(EnvelopeConverterFactory())
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
    fun unwraps_list_from_envelope() =
        runTest {
            server.enqueue(
                MockResponse().setBody("""{"statusCode":200,"message":"OK","data":[{"name":"a"},{"name":"b"}]}"""),
            )
            assertEquals(listOf(Item("a"), Item("b")), api.list())
        }

    @Test
    fun null_data_throws_ApiException_with_server_message() =
        runTest {
            server.enqueue(
                MockResponse().setBody("""{"statusCode":200,"message":"Kosong","data":null}"""),
            )
            val error = runCatching { api.item() }.exceptionOrNull() as ApiException
            assertEquals("Kosong", error.message)
        }

    @Test
    fun null_data_for_a_list_return_type_is_an_empty_list() =
        runTest {
            server.enqueue(MockResponse().setBody("""{"statusCode":200,"message":"OK","data":null}"""))
            assertEquals(emptyList<Item>(), api.list())
        }

    @Test
    fun explicit_envelope_type_is_left_alone() =
        runTest {
            server.enqueue(
                MockResponse().setBody("""{"statusCode":201,"message":"Dibuat","data":{"name":"x"}}"""),
            )
            assertEquals(ApiEnvelope(201, "Dibuat", Item("x")), api.wrapped())
        }
}

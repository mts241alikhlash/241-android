package id.sch.mts241alikhlash.cbt.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdInterceptorTest {
    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() = server.shutdown()

    private fun client(deviceId: () -> String) =
        OkHttpClient.Builder().addInterceptor(DeviceIdInterceptor(deviceId)).build()

    @Test
    fun adds_the_device_header_to_every_request() {
        server.enqueue(MockResponse().setBody("{}"))
        server.enqueue(MockResponse().setBody("{}"))
        val http = client { "device-1" }

        http.newCall(Request.Builder().url(server.url("/me/exams")).build()).execute().close()
        http.newCall(Request.Builder().url(server.url("/student-exams/s1/answers")).build()).execute().close()

        assertEquals("device-1", server.takeRequest().getHeader("X-Device-Id"))
        assertEquals("device-1", server.takeRequest().getHeader("X-Device-Id"))
    }

    @Test
    fun reads_the_id_on_each_request() {
        server.enqueue(MockResponse().setBody("{}"))
        server.enqueue(MockResponse().setBody("{}"))
        var current = "a"
        val http = client { current }

        http.newCall(Request.Builder().url(server.url("/x")).build()).execute().close()
        current = "b"
        http.newCall(Request.Builder().url(server.url("/x")).build()).execute().close()

        assertEquals("a", server.takeRequest().getHeader("X-Device-Id"))
        assertEquals("b", server.takeRequest().getHeader("X-Device-Id"))
    }

    @Test
    fun leaves_the_header_off_when_the_id_is_blank() {
        server.enqueue(MockResponse().setBody("{}"))

        client { "" }.newCall(Request.Builder().url(server.url("/x")).build()).execute().close()

        val sent = server.takeRequest()

        assertEquals(null, sent.getHeader("X-Device-Id"))
        assertEquals("android", sent.getHeader("X-Client-Platform"))
    }
}

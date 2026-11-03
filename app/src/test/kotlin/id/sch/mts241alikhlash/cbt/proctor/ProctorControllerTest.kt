package id.sch.mts241alikhlash.cbt.proctor

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import id.sch.mts241alikhlash.cbt.data.model.CameraBlockedBody
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProctorControllerTest {

    private class TestOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private class FakeSource : SnapshotSource {
        var frame: ByteArray? = byteArrayOf(1, 2, 3)
        var frameAt: Long = 0L
        var cleared = 0
        var started = 0

        override fun start(owner: LifecycleOwner, onBlocked: (String) -> Unit) {
            started++
        }

        override fun captureNow() = Unit

        override fun latestJpeg(): ByteArray? = frame

        override fun latestAt(): Long = frameAt

        override fun clear() {
            cleared++
            frame = null
            frameAt = 0L
        }

        override fun stop() = Unit
    }

    private class FakeApi(
        private val failures: Int = 0,
        private val failKinds: Set<String> = emptySet(),
    ) : ProctorSnapshotApi {
        val kinds = mutableListOf<String>()
        var attempts = 0
        val blockedReasons = java.util.concurrent.CopyOnWriteArrayList<String>()
        val blockedLatch = java.util.concurrent.CountDownLatch(1)

        override suspend fun uploadSnapshot(
            sessionId: String,
            kind: RequestBody,
            capturedAt: RequestBody,
            file: MultipartBody.Part,
        ) {
            attempts++
            val name = okio.Buffer().also { kind.writeTo(it) }.readUtf8()
            if (attempts <= failures || name in failKinds) throw IllegalStateException("offline")
            kinds += name
        }

        override suspend fun reportCameraBlocked(
            sessionId: String,
            request: CameraBlockedBody,
        ) {
            blockedReasons += request.reason
            blockedLatch.countDown()
        }
    }

    private fun controller(
        source: FakeSource,
        api: FakeApi,
        busy: () -> Boolean = { false },
    ) = ProctorController(api, busy, source)

    @Test
    fun `a tick with a fresh frame uploads the session start`() = runBlocking {
        val source = FakeSource().apply { frameAt = 14_000L }
        val api = FakeApi()
        controller(source, api).tick()
        assertEquals(listOf("SESSION_START"), api.kinds)
    }

    @Test
    fun `a tick without a new frame uploads nothing`() = runBlocking {
        val source = FakeSource().apply { frameAt = 14_000L }
        val api = FakeApi()
        val proctor = controller(source, api)
        proctor.tick()
        proctor.tick()
        assertEquals(1, api.attempts)
    }

    @Test
    fun `a newer frame uploads again on the next tick`() = runBlocking {
        val source = FakeSource().apply { frameAt = 14_000L }
        val api = FakeApi()
        val proctor = controller(source, api)
        proctor.tick()
        source.frameAt = 29_000L
        proctor.tick()
        assertEquals(listOf("SESSION_START", "LIVE"), api.kinds)
    }

    @Test
    fun `a reused controller starts the next session with a session start`() = runBlocking {
        val source = FakeSource().apply {
            frame = byteArrayOf(1)
            frameAt = 0L
        }
        val api = FakeApi()
        val proctor = controller(source, api)
        val owner = TestOwner()

        proctor.start(owner, "session-1")
        proctor.stop()
        proctor.start(owner, "session-2")
        proctor.stop()

        source.frameAt = 29_000L
        proctor.tick()
        assertEquals(listOf("SESSION_START"), api.kinds)
    }

    @Test
    fun `a stale frame after the camera stops uploads nothing`() = runBlocking {
        val source = FakeSource().apply { frameAt = 14_000L }
        val api = FakeApi()
        val proctor = controller(source, api)
        proctor.tick()
        source.clear()
        proctor.tick()
        assertEquals(1, api.attempts)
        assertEquals(1, source.cleared)
    }

    @Test
    fun `an empty frame does not consume the session start`() = runBlocking {
        val source = FakeSource().apply {
            frame = null
            frameAt = 14_000L
        }
        val api = FakeApi()
        val proctor = controller(source, api)
        proctor.tick()
        source.frame = byteArrayOf(9)
        proctor.tick()
        assertEquals(listOf("SESSION_START"), api.kinds)
    }

    @Test
    fun `a failed evidence upload is retried on the next tick`() = runBlocking {
        val source = FakeSource().apply { frameAt = 14_000L }
        val api = FakeApi(failures = 1)
        val proctor = controller(source, api)
        proctor.tick()
        assertTrue(api.kinds.isEmpty())
        proctor.tick()
        assertEquals(listOf("SESSION_START"), api.kinds)
    }

    @Test
    fun `a busy answer sync skips the tick`() = runBlocking {
        val source = FakeSource().apply { frameAt = 14_000L }
        val api = FakeApi()
        controller(source, api) { true }.tick()
        assertTrue(api.attempts == 0)
    }

    @Test
    fun `a camera failure is reported as blocked`() = runBlocking {
        val source = FakeSource()
        val api = FakeApi()
        controller(source, api).blocked("session-1", "permission")
        assertTrue(api.blockedLatch.await(5, java.util.concurrent.TimeUnit.SECONDS))
        assertEquals(listOf("permission"), api.blockedReasons)
    }

    @Test
    fun `a failed live upload is not re-armed as an evidence kind`() = runBlocking {
        val source = FakeSource().apply { frameAt = 14_000L }
        val api = FakeApi(failKinds = setOf("LIVE"))
        val proctor = controller(source, api)
        proctor.tick()
        source.frameAt = 29_000L
        proctor.tick()
        proctor.tick()
        assertEquals(listOf("SESSION_START"), api.kinds)
        assertEquals(3, api.attempts)
    }
}

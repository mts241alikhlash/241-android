package id.sch.mts241alikhlash.exam.kiosk

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViolationStoreTest {
    private lateinit var store: ViolationStore

    @Before
    fun setUp() {
        store = ViolationStore(ApplicationProvider.getApplicationContext())
        store.reset()
    }

    @Test
    fun `records increment the counter`() {
        assertEquals(0, store.count())
        assertEquals(1, store.record("pin_lost"))
        assertEquals(2, store.record("left_app"))
        assertEquals(2, store.count())
    }

    @Test
    fun `entries keep the newest event first with its reason`() {
        store.record("pin_lost")
        store.record("supervisor_exit")

        val entries = store.entries()
        assertEquals(2, entries.size)
        assertTrue(entries[0].endsWith("|supervisor_exit"))
        assertTrue(entries[1].endsWith("|pin_lost"))
    }

    @Test
    fun `reset clears counter and log`() {
        store.record("left_app")
        store.reset()

        assertEquals(0, store.count())
        assertTrue(store.entries().isEmpty())
    }

    @Test
    fun `a finished exam leaves no count behind for the next one`() {
        store.beginSession("s1")
        store.record("left_app")
        store.restore(3)

        store.reset()

        assertEquals(0, store.counted())
        store.beginSession("s1")
        assertEquals(0, store.counted())
    }

    @Test
    fun `the counted total leaves out what a supervisor allowed or refused`() {
        store.record("left_app")
        store.record("supervisor_exit")
        store.record("supervisor_exit_denied")
        store.record("pin_lost")

        assertEquals(2, store.counted())
    }

    @Test
    fun `a session keeps its count when it resumes and starts again from zero when another begins`() {
        store.beginSession("s1")
        store.record("left_app")
        store.record("left_app")

        store.beginSession("s1")
        assertEquals(2, store.counted())

        store.beginSession("s2")
        assertEquals(0, store.counted())
    }

    @Test
    fun `restoring a higher count from the server raises what is counted`() {
        store.beginSession("s1")
        store.record("left_app")

        store.restore(3)

        assertEquals(3, store.counted())
    }

    @Test
    fun `restoring never lowers the count`() {
        store.beginSession("s1")
        store.record("left_app")
        store.record("pin_lost")

        store.restore(1)

        assertEquals(2, store.counted())
    }

    @Test
    fun `an event after a restore adds to the restored count`() {
        store.beginSession("s1")
        store.restore(3)

        store.record("left_app")

        assertEquals(4, store.counted())
    }

    @Test
    fun `a restore of the same session is not counted twice`() {
        store.beginSession("s1")
        store.restore(3)
        store.restore(3)

        assertEquals(3, store.counted())
    }

    @Test
    fun `a new session starts without the restored count of the old one`() {
        store.beginSession("s1")
        store.restore(3)

        store.beginSession("s2")

        assertEquals(0, store.counted())
    }
}

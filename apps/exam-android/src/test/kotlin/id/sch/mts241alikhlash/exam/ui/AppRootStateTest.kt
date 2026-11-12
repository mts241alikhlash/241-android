package id.sch.mts241alikhlash.exam.ui

import id.sch.mts241alikhlash.exam.data.auth.AuthUser
import id.sch.mts241alikhlash.exam.myExam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRootStateTest {
    private val user = AuthUser("u1", "Ahmad", setOf("STUDENT"))
    private val exam = myExam(id = "e1", title = "UTS", subjectId = "Matematika", startsAt = "a", endsAt = "b")

    private fun states() =
        listOf(
            AppRootState(
                restoreComplete = true,
                restoreError = "gagal memulihkan",
                user = user,
                sessionId = "s1",
                deadline = 1_700_000_000_000L,
                activeExam = exam,
            ),
            AppRootState(
                restoreComplete = true,
                user = user,
                sessionId = "s1",
                reentrySessionId = "s1",
                reentryLoading = true,
                reentryError = "gagal",
            ),
            AppRootState(
                restoreComplete = true,
                user = user,
                sessionId = "s1",
                selectedExam = exam,
                readinessComplete = true,
                starting = true,
                startError = "gagal",
            ),
            AppRootState(
                restoreComplete = true,
                user = user,
                reentrySessionId = "s1",
                selectedExam = exam,
                activeExam = exam,
            ),
        )

    @Test
    fun `every way out of the exam lands on the list with one clean state`() {
        for (before in states()) {
            val after = before.afterRelease("Ujian berhasil dikumpulkan")

            assertNull(after.sessionId)
            assertNull(after.deadline)
            assertNull(after.restoreError)
            assertNull(after.reentrySessionId)
            assertNull(after.selectedExam)
            assertNull(after.activeExam)
            assertFalse(after.reentryLoading)
            assertNull(after.reentryError)
            assertFalse(after.starting)
            assertNull(after.startError)
            assertFalse(after.readinessComplete)
            assertEquals("Ujian berhasil dikumpulkan", after.notice)
        }
    }

    @Test
    fun `a release keeps the signed in student and the restore result`() {
        val before = states().first()

        val after = before.afterRelease()

        assertEquals(user, after.user)
        assertTrue(after.restoreComplete)
    }

    @Test
    fun `a release without a notice drops the previous one`() {
        val after = states().first().copy(notice = "Ujian dijeda").afterRelease()

        assertNull(after.notice)
    }
}

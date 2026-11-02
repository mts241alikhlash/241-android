package id.sch.mts241alikhlash.cbt.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import id.sch.mts241alikhlash.cbt.data.auth.AuthUser
import id.sch.mts241alikhlash.cbt.data.model.ExamSummaryDto
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class AppRootRoutingTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
    )

    private val user = AuthUser("u1", "Ahmad", setOf("STUDENT"))
    private val exam = ExamSummaryDto("e1", "UTS", "Matematika", "a", "b", 90, "OPEN")

    private fun screenFor(state: AppRootState): String {
        var shown = ""
        paparazzi.snapshot {
            AssessmentAppRoot(
                state = state,
                onRetryRestore = {},
                onStartExam = {},
                examContent = { _, _ -> shown = "exam" },
                loginContent = { shown = "login" },
                examListContent = { shown = "list" },
                readinessContent = { shown = "readiness" },
                tokenContent = { shown = "token:${it.id}" }
            )
        }
        return shown
    }

    @Test
    fun root_shows_login_when_no_user_and_no_session() {
        assertEquals("login", screenFor(AppRootState(restoreComplete = true)))
    }

    @Test
    fun root_shows_exam_list_when_user_without_selected_exam() {
        assertEquals("list", screenFor(AppRootState(restoreComplete = true, user = user)))
    }

    @Test
    fun root_shows_token_screen_when_exam_selected() {
        assertEquals(
            "token:e1",
            screenFor(
                AppRootState(
                    restoreComplete = true,
                    user = user,
                    selectedExam = exam,
                    readinessComplete = true
                )
            )
        )
    }

    @Test
    fun root_gates_token_screen_behind_readiness_confirmation() {
        var state by mutableStateOf(
            AppRootState(restoreComplete = true, user = user, selectedExam = exam)
        )
        var shown = ""
        var confirm: (() -> Unit)? = null

        paparazzi.snapshot {
            AssessmentAppRoot(
                state = state,
                onRetryRestore = {},
                onStartExam = {},
                examContent = { _, _ -> shown = "exam" },
                examListContent = { shown = "list" },
                readinessContent = { ready ->
                    shown = "readiness"
                    confirm = ready
                },
                onReadinessConfirmed = { state = state.copy(readinessComplete = true) },
                tokenContent = { shown = "token:${it.id}" }
            )
        }

        assertEquals("readiness", shown)
        assertNotNull(confirm)

        confirm?.invoke()
        paparazzi.snapshot {
            AssessmentAppRoot(
                state = state,
                onRetryRestore = {},
                onStartExam = {},
                examContent = { _, _ -> shown = "exam" },
                examListContent = { shown = "list" },
                readinessContent = { shown = "readiness" },
                onReadinessConfirmed = {},
                tokenContent = { shown = "token:${it.id}" }
            )
        }

        assertEquals("token:e1", shown)
    }

    @Test
    fun root_shows_the_token_screen_when_reentry_is_required() {
        var resumed = ""

        paparazzi.snapshot {
            AssessmentAppRoot(
                state = AppRootState(
                    restoreComplete = true,
                    sessionId = "s1",
                    deadline = 1_700_005_400_000L,
                    reentrySessionId = "s1"
                ),
                onRetryRestore = {},
                onStartExam = {},
                onResumeWithToken = { resumed = it },
                examContent = { _, _ -> error("the paper must stay closed") },
                tokenContent = { error("the start token screen must not show") }
            )
        }

        assertEquals("", resumed)
    }

    @Test
    fun root_shows_exam_content_when_session_restored_without_user() {
        assertEquals(
            "exam",
            screenFor(AppRootState(restoreComplete = true, sessionId = "s1"))
        )
    }
}

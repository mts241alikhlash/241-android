package id.sch.mts241alikhlash.istiqra.ui.screens

import id.sch.mts241alikhlash.istiqra.MainDispatcherRule
import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.data.auth.httpError
import id.sch.mts241alikhlash.istiqra.data.repository.ExamCatalogRepository
import id.sch.mts241alikhlash.istiqra.myExam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ExamListViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun exam(
        id: String,
        status: String,
    ) = myExam(
        id = id,
        title = "Ujian $id",
        subjectId = "Matematika",
        startsAt = "2026-11-01T01:00:00Z",
        endsAt = "2026-11-01T03:00:00Z",
        status = MyExamDto.Status.valueOf(status),
    )

    private class Api(
        var result: () -> List<MyExamDto>,
    ) : StubExamApi() {
        override suspend fun listExams(): List<MyExamDto> = result()
    }

    private fun viewModel(api: Api) = ExamListViewModel(ExamCatalogRepository(api, { "device-1" }))

    @Test
    fun list_empty_state_when_no_exams() {
        assertEquals(ExamListUiState.Empty, viewModel(Api { emptyList() }).state.value)
    }

    @Test
    fun list_content_state_when_exams_exist() {
        val exams = listOf(exam("e1", "OPEN"))
        assertEquals(ExamListUiState.Content(exams), viewModel(Api { exams }).state.value)
    }

    @Test
    fun list_error_state_on_network_failure_with_retry() {
        val api = Api { throw IOException("offline") }
        val vm = viewModel(api)

        assertEquals(
            ExamListUiState.Error("Tidak dapat terhubung ke server.", unauthorized = false),
            vm.state.value,
        )

        api.result = { listOf(exam("e1", "OPEN")) }
        vm.refresh()

        assertTrue(vm.state.value is ExamListUiState.Content)
    }

    @Test
    fun reload_replaces_a_stale_list_with_the_new_statuses() {
        val api = Api { listOf(exam("e1", "OPEN")) }
        val vm = viewModel(api)

        api.result = { listOf(exam("e1", "FINISHED")) }
        vm.reload()

        assertEquals(ExamListUiState.Content(listOf(exam("e1", "FINISHED"))), vm.state.value)
    }

    @Test
    fun reload_failure_keeps_the_last_list_with_a_notice() {
        val api = Api { listOf(exam("e1", "OPEN")) }
        val vm = viewModel(api)

        api.result = { throw IOException("offline") }
        vm.reload()

        assertEquals(
            ExamListUiState.Content(listOf(exam("e1", "OPEN")), "Tidak dapat terhubung ke server."),
            vm.state.value,
        )
    }

    @Test
    fun reload_unauthorized_drops_the_list() {
        val api = Api { listOf(exam("e1", "OPEN")) }
        val vm = viewModel(api)

        api.result = { throw httpError(401) }
        vm.reload()

        assertEquals(true, (vm.state.value as ExamListUiState.Error).unauthorized)
    }

    @Test
    fun list_error_marks_unauthorized_on_401() {
        val vm = viewModel(Api { throw httpError(401) })

        assertEquals(true, (vm.state.value as ExamListUiState.Error).unauthorized)
    }

    @Test
    fun only_open_or_in_progress_exams_selectable() {
        assertTrue(exam("a", "OPEN").isSelectable())
        assertTrue(exam("b", "IN_PROGRESS").isSelectable())
        assertFalse(exam("c", "UPCOMING").isSelectable())
        assertFalse(exam("d", "FINISHED").isSelectable())
    }

    @Test
    fun a_handed_in_exam_is_finished_while_its_window_is_still_open() {
        val handedIn = exam("a", "FINISHED").copy(lifecycle = MyExamDto.Lifecycle.OPEN)

        assertFalse(handedIn.isSelectable())
        assertFalse(handedIn.isExpandable())
        assertEquals(listOf("Selesai"), groupExams(listOf(handedIn)).map { it.title })
    }

    @Test
    fun an_open_window_still_lets_a_student_start_an_exam_not_yet_taken() {
        val open = exam("a", "UPCOMING").copy(lifecycle = MyExamDto.Lifecycle.OPEN)

        assertTrue(open.isSelectable())
        assertEquals(listOf("Bisa Dikerjakan"), groupExams(listOf(open)).map { it.title })
    }

    @Test
    fun lifecycle_finished_makes_an_open_looking_status_unstartable() {
        val finished = exam("b", "OPEN").copy(lifecycle = MyExamDto.Lifecycle.FINISHED)

        assertFalse(finished.isSelectable())
        assertFalse(finished.isExpandable())
        assertEquals(listOf("Selesai"), groupExams(listOf(finished)).map { it.title })
    }

    @Test
    fun an_old_server_without_lifecycle_still_uses_status() {
        assertTrue(exam("a", "OPEN").isSelectable())
        assertEquals(listOf("Selesai"), groupExams(listOf(exam("b", "FINISHED"))).map { it.title })
        assertEquals(listOf("Akan Datang"), groupExams(listOf(exam("c", "UPCOMING"))).map { it.title })
    }
}

class ExamStatusLabelTest {
    private val zone = java.util.TimeZone.getTimeZone("Asia/Jakarta")

    @org.junit.Test
    fun upcoming_shows_the_local_opening_time() {
        val label = examStatusLabel("UPCOMING", "2026-11-02T01:00:00.000Z", zone)
        org.junit.Assert.assertTrue(label, label.startsWith("Dibuka "))
        org.junit.Assert.assertTrue(label, label.contains("08.00"))
        org.junit.Assert.assertTrue(label, label.contains("2026"))
    }

    @org.junit.Test
    fun in_progress_invites_the_student_to_continue() {
        org.junit.Assert.assertEquals("Sedang berlangsung · Lanjutkan", examStatusLabel("IN_PROGRESS", "x", zone))
    }

    @org.junit.Test
    fun open_and_finished_keep_their_labels() {
        org.junit.Assert.assertEquals("Dibuka · Mulai", examStatusLabel("OPEN", "x", zone))
        org.junit.Assert.assertEquals("Selesai", examStatusLabel("FINISHED", "x", zone))
    }

    @org.junit.Test
    fun an_unreadable_start_time_falls_back_to_the_plain_label() {
        org.junit.Assert.assertEquals("Belum dibuka", examStatusLabel("UPCOMING", "bukan tanggal", zone))
    }
}

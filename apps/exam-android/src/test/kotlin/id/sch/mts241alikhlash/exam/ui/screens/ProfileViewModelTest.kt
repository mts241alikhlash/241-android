package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.MainDispatcherRule
import id.sch.mts241alikhlash.exam.data.auth.httpError
import id.sch.mts241alikhlash.exam.data.model.MyClassroomDto
import id.sch.mts241alikhlash.exam.data.model.StudentMeDto
import id.sch.mts241alikhlash.exam.data.remote.StudentApiService
import id.sch.mts241alikhlash.exam.data.repository.StudentProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ProfileViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private class Api(
        var meResult: () -> StudentMeDto = { StudentMeDto(id = "s1", nis = "2024001") },
        var classroomResult: () -> MyClassroomDto? = { null },
    ) : StudentApiService {
        override suspend fun me(): StudentMeDto = meResult()

        override suspend fun classroom(): MyClassroomDto? = classroomResult()
    }

    private fun viewModel(api: Api) = ProfileViewModel(StudentProfileRepository(api), "Ahmad")

    @Test
    fun `loads the profile with the session name as the fallback`() {
        val state = viewModel(Api()).state.value

        assertTrue(state is ProfileUiState.Content)
        assertEquals("Ahmad", (state as ProfileUiState.Content).profile.name)
    }

    @Test
    fun `a failing classroom call does not fail the profile`() {
        val api = Api(classroomResult = { throw IOException("offline") })

        assertTrue(viewModel(api).state.value is ProfileUiState.Content)
    }

    @Test
    fun `a failing student call shows an error that retry clears`() {
        val api = Api(meResult = { throw httpError(500) })
        val vm = viewModel(api)

        assertTrue(vm.state.value is ProfileUiState.Error)

        api.meResult = { StudentMeDto(id = "s1", nis = "2024001") }
        vm.load()

        assertTrue(vm.state.value is ProfileUiState.Content)
    }

    @Test
    fun `a later failure keeps the profile already shown`() {
        val api = Api()
        val vm = viewModel(api)

        api.meResult = { throw IOException("offline") }
        vm.load()

        assertTrue(vm.state.value is ProfileUiState.Content)
    }
}

package id.sch.mts241alikhlash.istiqra.ui.screens

import id.sch.mts241alikhlash.istiqra.MainDispatcherRule
import id.sch.mts241alikhlash.istiqra.api.student.apis.StudentsApi
import id.sch.mts241alikhlash.istiqra.api.student.models.MyClassroomResponseDto
import id.sch.mts241alikhlash.istiqra.api.student.models.StudentControllerFindMine200Response
import id.sch.mts241alikhlash.istiqra.api.student.models.StudentControllerFindMyClassroom200Response
import id.sch.mts241alikhlash.istiqra.api.student.models.StudentInternalControllerParticipantNumber200Response
import id.sch.mts241alikhlash.istiqra.api.student.models.StudentResponseDto
import id.sch.mts241alikhlash.istiqra.api.student.models.StudentResponseUserDto
import id.sch.mts241alikhlash.istiqra.data.auth.httpError
import id.sch.mts241alikhlash.istiqra.data.repository.StudentProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

private fun testStudent(
    id: String = "s1",
    nis: String = "2024001",
) = StudentResponseDto(
    id = id,
    userId = "u1",
    nis = nis,
    nisn = "",
    status = StudentResponseDto.Status.ACTIVE,
    user = StudentResponseUserDto(id = "u1", identifier = nis, isActive = true),
)

class ProfileViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private class Api(
        var meResult: () -> StudentResponseDto = { testStudent() },
        var classroomResult: () -> MyClassroomResponseDto? = { null },
    ) : StudentsApi {
        override suspend fun studentControllerFindMine(): Response<StudentControllerFindMine200Response> = Response.success(StudentControllerFindMine200Response(data = meResult()))

        override suspend fun studentControllerFindMyClassroom(): Response<StudentControllerFindMyClassroom200Response> = Response.success(StudentControllerFindMyClassroom200Response(data = classroomResult()))

        override suspend fun studentControllerFindMyParticipantNumber(): Response<StudentInternalControllerParticipantNumber200Response> = throw UnsupportedOperationException()
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

        api.meResult = { testStudent() }
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

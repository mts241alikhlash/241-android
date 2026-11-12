package id.sch.mts241alikhlash.istiqra.data.repository

import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StartSessionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StartedSessionDto
import id.sch.mts241alikhlash.istiqra.data.remote.ApiException
import id.sch.mts241alikhlash.istiqra.data.session.ServerClock
import id.sch.mts241alikhlash.istiqra.myExam
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ExamCatalogRepositoryTest {
    private val exam =
        myExam(
            id = "e1",
            title = "UTS Matematika",
            subjectId = "s1",
            startsAt = "2026-11-01T01:00:00Z",
            endsAt = "2026-11-01T03:00:00Z",
            durationMinutes = 90,
            status = MyExamDto.Status.OPEN,
        )

    private class FakeCatalogApi(
        var exams: List<MyExamDto> = emptyList(),
        var started: () -> StartedSessionDto = { StartedSessionDto("s1", 0, 0) },
    ) : StubExamApi() {
        var lastRequest: StartSessionDto? = null

        override suspend fun listExams(): List<MyExamDto> = exams

        override suspend fun startSession(
            examId: String,
            request: StartSessionDto,
        ): StartedSessionDto {
            lastRequest = request
            return started()
        }
    }

    @Test
    fun listExams_returns_api_list() =
        runTest {
            val api = FakeCatalogApi(exams = listOf(exam))
            assertEquals(listOf(exam), ExamCatalogRepository(api, { "device-1" }).listExams())
        }

    @Test
    fun startSession_keeps_the_server_deadline_once_the_clock_is_synced() =
        runTest {
            val api = FakeCatalogApi(started = { StartedSessionDto("s1", deadline = 65_000L, serverTime = 5_000L) })
            val clock = ServerClock { 0L }
            val repo = ExamCatalogRepository(api, { "device-1" }, clock = clock)

            val session = repo.startSession("e1", "ABC123")

            assertEquals("s1", session.sessionId)
            assertEquals(65_000L, session.deadlineLocalMillis)
            assertEquals(5_000L, clock.nowMillis())
            assertEquals(StartSessionDto("ABC123", "device-1"), api.lastRequest)
        }

    @Test
    fun startSession_409_surfaces_server_message() =
        runTest {
            val api =
                FakeCatalogApi(started = {
                    throw HttpException(
                        Response.error<Any>(
                            409,
                            """{"statusCode":409,"message":"Sesi aktif di perangkat lain","data":null}""".toResponseBody(),
                        ),
                    )
                })
            val repo = ExamCatalogRepository(api, { "device-1" })

            val error = runCatching { repo.startSession("e1", "ABC123") }.exceptionOrNull() as ApiException

            assertEquals(409, error.statusCode)
            assertEquals("Sesi aktif di perangkat lain", error.message)
        }
}

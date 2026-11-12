package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.api.exam.apis.StudentExamsApi
import id.sch.mts241alikhlash.exam.api.exam.models.ExamPolicyDto
import id.sch.mts241alikhlash.exam.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.exam.api.exam.models.StartSessionDto
import id.sch.mts241alikhlash.exam.data.remote.guarded
import id.sch.mts241alikhlash.exam.data.remote.unwrapData
import id.sch.mts241alikhlash.exam.data.session.ServerClock

data class StartedSession(
    val sessionId: String,
    val deadlineLocalMillis: Long,
    val policy: ExamPolicyDto = ExamPolicyDto(),
)

class ExamCatalogRepository(
    private val api: StudentExamsApi,
    private val deviceId: () -> String,
    private val clock: ServerClock = ServerClock(),
) {
    suspend fun listExams(): List<MyExamDto> =
        guarded {
            api.studentExamControllerFindMine().unwrapData { it.data }
        }

    suspend fun startSession(
        examId: String,
        accessCode: String,
    ): StartedSession {
        val started =
            guarded {
                api
                    .studentExamControllerStart(
                        examId,
                        StartSessionDto(accessCode.trim(), deviceId()),
                    ).unwrapData { it.data }
            }
        clock.syncEpochMillis(started.serverTime)
        return StartedSession(
            sessionId = started.sessionId,
            deadlineLocalMillis = started.deadline,
            policy = started.policy,
        )
    }
}

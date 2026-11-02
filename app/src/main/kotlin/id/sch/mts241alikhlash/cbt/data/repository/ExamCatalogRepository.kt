package id.sch.mts241alikhlash.cbt.data.repository

import id.sch.mts241alikhlash.cbt.data.model.ExamSummaryDto
import id.sch.mts241alikhlash.cbt.data.model.StartSessionRequest
import id.sch.mts241alikhlash.cbt.data.remote.ExamCatalogApiService
import id.sch.mts241alikhlash.cbt.data.remote.guarded
import id.sch.mts241alikhlash.cbt.data.session.ServerClock

data class StartedSession(val sessionId: String, val deadlineLocalMillis: Long)

class ExamCatalogRepository(
    private val api: ExamCatalogApiService,
    private val deviceId: () -> String,
    private val clock: ServerClock = ServerClock()
) {
    suspend fun listExams(): List<ExamSummaryDto> = guarded { api.listExams() }

    suspend fun startSession(examId: String, accessCode: String): StartedSession {
        val started = guarded {
            api.startSession(examId, StartSessionRequest(accessCode.trim(), deviceId()))
        }
        clock.syncEpochMillis(started.serverTime)
        return StartedSession(
            sessionId = started.sessionId,
            deadlineLocalMillis = started.deadline
        )
    }
}

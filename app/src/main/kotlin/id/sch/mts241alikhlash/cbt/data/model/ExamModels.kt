package id.sch.mts241alikhlash.cbt.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExamSummaryDto(
    val id: String,
    val title: String,
    val subjectId: String,
    val startsAt: String,
    val endsAt: String,
    val durationMinutes: Int,
    val status: String,
    val sessionId: String? = null,
    val subjectName: String? = null,
    val type: String? = null,
    val participantNumber: String? = null,
    val minSubmitMinutes: Int = 0
)

@Serializable
data class StartSessionRequest(
    val accessCode: String,
    val deviceId: String
)

@Serializable
data class SessionPaperDto(
    val questions: List<PaperQuestionDto>,
    val startedAt: String,
    val minSubmitMinutes: Int = 0,
    val serverTime: String? = null
)

@Serializable
data class StartedSessionDto(
    val sessionId: String,
    val deadline: Long,
    val serverTime: Long
)

@Serializable
data class SubmitRequest(
    val violations: Int = 0,
    val expectedAnswerCount: Int? = null,
    val pendingAnswers: List<SaveAnswerItemDto>? = null
)

@Serializable
data class ViolationReport(
    val count: Int
)

@Serializable
data class ViolationCountDto(
    val violationCount: Int
)

@Serializable
data class SubmittedSessionDto(
    val status: String,
    val submittedAt: String? = null,
    val confirmedAnswerCount: Int = 0,
    val answersStored: Boolean = true,
    val serverTime: String? = null
)

@Serializable
data class CameraBlockedBody(
    val reason: String
)

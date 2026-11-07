package id.sch.mts241alikhlash.exam.data.model

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
    val minSubmitMinutes: Int = 0,
)

@Serializable
data class StartSessionRequest(
    val accessCode: String,
    val deviceId: String,
)

@Serializable
data class SessionPaperDto(
    val questions: List<PaperQuestionDto>,
    val startedAt: String,
    val minSubmitMinutes: Int = 0,
    val serverTime: String? = null,
)

@Serializable
data class ExamPolicyDto(
    val violationLimit: Int = 3,
    val focusThresholdMs: Long = 10_000L,
    val snapshotIntervalMs: Long = 15_000L,
    val heartbeatIntervalMs: Long = 15_000L,
    val gapRecordedMs: Long = 60_000L,
    val gapTokenMs: Long = 300_000L,
    val gapViolationFrom: Int = 2,
    val tokenSlotSeconds: Int = 600,
)

@Serializable
data class StartedSessionDto(
    val sessionId: String,
    val deadline: Long,
    val serverTime: Long,
    val policy: ExamPolicyDto = ExamPolicyDto(),
)

@Serializable
data class SubmitRequest(
    val violations: Int = 0,
    val expectedAnswerCount: Int? = null,
    val pendingAnswers: List<SaveAnswerItemDto>? = null,
)

@Serializable
data class ViolationReport(
    val count: Int,
)

@Serializable
data class ViolationCountDto(
    val violationCount: Int,
)

@Serializable
data class SubmittedSessionDto(
    val status: String,
    val submittedAt: String? = null,
    val confirmedAnswerCount: Int = 0,
    val answersStored: Boolean = true,
    val serverTime: String? = null,
)

@Serializable
data class CameraBlockedBody(
    val reason: String,
)

const val SUSPENSION_REASON_CALL = "CALL"

@Serializable
data class SuspensionRequest(
    val reason: String,
    val seconds: Int,
    val clientId: String,
)

@Serializable
data class SuspensionResponseDto(
    val deadline: String,
    val finalizeAt: String? = null,
    val extraMinutes: Int = 0,
    val suspendedSeconds: Int = 0,
)

package id.sch.mts241alikhlash.cbt.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionEventUploadItem(
    val clientId: String,
    val type: String,
    val isViolation: Boolean,
    val detail: String? = null,
    val occurredAt: String
)

@Serializable
data class RecordSessionEventsRequest(
    val events: List<SessionEventUploadItem>
)

@Serializable
data class SessionEventDto(
    val id: String,
    val clientId: String,
    val type: String,
    val isViolation: Boolean,
    val detail: String? = null,
    val occurredAt: String
)

@Serializable
data class RecordSessionEventsResponse(
    val events: List<SessionEventDto> = emptyList(),
    val violationCount: Int = 0,
    val requiresTokenReentry: Boolean = false,
    val lockedByViolation: Boolean = false,
    val serverTime: String? = null
)

@Serializable
data class SessionHeartbeatResponse(
    val lockedByViolation: Boolean = false,
    val requiresTokenReentry: Boolean = false,
    val absentSince: String? = null,
    val serverTime: String? = null
)

@Serializable
data class ResumeSessionRequest(
    val accessCode: String,
    val deviceId: String
)

@Serializable
data class ResumedSessionDto(
    val sessionId: String,
    val status: String,
    val deadline: String,
    val serverTime: String,
    val requiresTokenReentry: Boolean = false
)

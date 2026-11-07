package id.sch.mts241alikhlash.exam.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PaperChoiceDto(
    val key: String,
    val html: String,
)

@Serializable
data class PaperQuestionDto(
    val id: String,
    val number: Int,
    val type: String,
    val bodyHtml: String,
    val points: Double,
    val choices: List<PaperChoiceDto>? = null,
)

@Serializable
data class SaveAnswerItemDto(
    val questionId: String,
    val clientId: String,
    val answer: JsonObject,
)

@Serializable
data class SaveAnswersBatchRequest(
    val answers: List<SaveAnswerItemDto>,
    val pendingLocalCount: Int? = null,
    val batteryLevel: Int? = null,
    val isCharging: Boolean? = null,
    val dndGranted: Boolean? = null,
    val lockTaskActive: Boolean? = null,
    val batteryUnrestricted: Boolean? = null,
)

@Serializable
data class ConfirmedAnswerDto(
    val questionId: String,
    val clientId: String,
    val updatedAt: String,
    val receivedAfterDeadline: Boolean,
)

@Serializable
data class SkippedAnswerDto(
    val questionId: String,
    val clientId: String,
)

@Serializable
data class SaveAnswersBatchResultDto(
    val confirmed: List<ConfirmedAnswerDto> = emptyList(),
    val skipped: List<SkippedAnswerDto> = emptyList(),
    val confirmedCount: Int = 0,
    val serverTime: String? = null,
)

@Serializable
data class SavedAnswerDto(
    val questionId: String,
    val answer: JsonObject,
)

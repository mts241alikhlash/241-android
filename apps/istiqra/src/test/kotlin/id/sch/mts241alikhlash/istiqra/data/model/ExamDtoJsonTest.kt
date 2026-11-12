package id.sch.mts241alikhlash.istiqra.data.model

import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperChoiceDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswerItemDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SavedAnswerDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StartedSessionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.istiqra.myExam
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExamDtoJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun exam_summary_reads_the_camel_case_wire_shape() {
        val dto =
            json.decodeFromString<MyExamDto>(
                """{"id":"e1","title":"UTS","subjectId":"s1","startsAt":"2026-11-02T01:00:00.000Z","endsAt":"2026-11-02T04:00:00.000Z","durationMinutes":90,"minSubmitMinutes":0,"status":"IN_PROGRESS","sessionId":"x","type":"MIDTERM","category":"EXAM","lifecycle":"OPEN","makeup":false}""",
            )
        assertEquals(
            myExam(
                id = "e1",
                title = "UTS",
                subjectId = "s1",
                startsAt = "2026-11-02T01:00:00.000Z",
                endsAt = "2026-11-02T04:00:00.000Z",
                durationMinutes = 90,
                status = MyExamDto.Status.IN_PROGRESS,
                sessionId = "x",
                type = MyExamDto.Type.MIDTERM,
                category = MyExamDto.Category.EXAM,
            ),
            dto,
        )
    }

    @Test
    fun exam_summary_allows_a_null_session() {
        val dto =
            json.decodeFromString<MyExamDto>(
                """{"id":"e1","title":"UTS","subjectId":"s1","startsAt":"a","endsAt":"b","durationMinutes":90,"minSubmitMinutes":0,"status":"OPEN","sessionId":null,"type":"MIDTERM","category":"EXAM","lifecycle":"OPEN","makeup":false}""",
            )
        assertNull(dto.sessionId)
    }

    @Test
    fun started_session_reads_epoch_values() {
        val dto = json.decodeFromString<StartedSessionDto>("""{"sessionId":"s1","deadline":1700000000000,"serverTime":1699999000000}""")
        assertEquals(StartedSessionDto("s1", 1700000000000L, 1699999000000L), dto)
    }

    @Test
    fun paper_question_reads_choices_with_keys() {
        val dto =
            json.decodeFromString<PaperQuestionDto>(
                """{"id":"q1","number":3,"type":"MULTIPLE_CHOICE","bodyHtml":"<p>x</p>","points":2,"choices":[{"key":"B","html":"<p>b</p>"},{"key":"A","html":"a"}]}""",
            )
        assertEquals(listOf(PaperChoiceDto.Key.B, PaperChoiceDto.Key.A), dto.choices?.map { it.key })
        assertEquals(2.0, dto.points, 0.0)
    }

    @Test
    fun paper_question_without_choices_has_null() {
        val dto = json.decodeFromString<PaperQuestionDto>("""{"id":"q1","number":1,"type":"ESSAY","bodyHtml":"x","points":5,"choices":null}""")
        assertNull(dto.choices)
    }

    @Test
    fun saved_answer_and_submitted() {
        val saved = json.decodeFromString<SavedAnswerDto>("""{"questionId":"q1","answer":{"keys":["A","B"]}}""")
        assertEquals(AnswerPayload.Multiple(listOf("A", "B")), AnswerPayload.fromJson(saved.answer))
        val done =
            json.decodeFromString<SubmitSessionResponseDto>(
                """{"status":"GRADED","submittedAt":null,"confirmedAnswerCount":46,"answersStored":true,"serverTime":"t"}""",
            )
        assertNull(done.submittedAt)
        assertEquals(46, done.confirmedAnswerCount)
    }

    @Test
    fun batch_response_reads_confirmed_and_skipped() {
        val result =
            json.decodeFromString<SaveAnswersBatchResultDto>(
                """{"confirmed":[{"questionId":"q1","clientId":"c1","updatedAt":"t","receivedAfterDeadline":false}],"skipped":[{"questionId":"q2","clientId":"c2"}],"confirmedCount":1,"serverTime":"s"}""",
            )
        assertEquals(listOf("q1"), result.confirmed.map { it.questionId })
        assertEquals(listOf("q2"), result.skipped.map { it.questionId })
        assertEquals(1, result.confirmedCount)
        assertEquals("s", result.serverTime)
    }

    @Test
    fun batch_request_serializes_camel_case() {
        val body =
            json.encodeToString(
                SaveAnswersBatchDto.serializer(),
                SaveAnswersBatchDto(
                    answers =
                        listOf(
                            SaveAnswerItemDto("q1", "c1", AnswerPayload.Choice("A").toJson()),
                        ),
                    pendingLocalCount = 4,
                    batteryLevel = 88,
                    isCharging = true,
                    dndGranted = true,
                    lockTaskActive = false,
                    batteryUnrestricted = true,
                ),
            )
        assertEquals(
            """{"answers":[{"questionId":"q1","clientId":"c1","answer":{"key":"A"}}],"batteryLevel":88,"isCharging":true,"dndGranted":true,"lockTaskActive":false,"batteryUnrestricted":true,"pendingLocalCount":4}""",
            body,
        )
    }

    @Test
    fun suspension_request_serializes_the_call_reason() {
        val body =
            json.encodeToString(
                SessionSuspensionRequestDto.serializer(),
                SessionSuspensionRequestDto(reason = SessionSuspensionRequestDto.Reason.CALL, seconds = 90, clientId = "c-1"),
            )
        assertEquals("""{"reason":"CALL","seconds":90,"clientId":"c-1"}""", body)
    }

    @Test
    fun suspension_response_reads_the_extended_deadline() {
        val dto =
            json.decodeFromString<SessionSuspensionDto>(
                """{"deadline":"2026-10-27T06:00:00.000Z","finalizeAt":"2026-10-27T06:10:00.000Z","extraMinutes":2,"suspendedSeconds":90}""",
            )
        assertEquals("2026-10-27T06:00:00.000Z", dto.deadline)
        assertEquals("2026-10-27T06:10:00.000Z", dto.finalizeAt)
        assertEquals(2, dto.extraMinutes)
        assertEquals(90, dto.suspendedSeconds)
    }

    @Test
    fun submit_request_sends_pending_answers_and_the_expected_count() {
        val body =
            json.encodeToString(
                SubmitSessionRequestDto.serializer(),
                SubmitSessionRequestDto(
                    violations = 2,
                    expectedAnswerCount = 46,
                    pendingAnswers =
                        listOf(
                            SaveAnswerItemDto("q1", "c1", AnswerPayload.Choice("A").toJson()),
                        ),
                ),
            )
        assertEquals(
            """{"violations":2,"expectedAnswerCount":46,"pendingAnswers":[{"questionId":"q1","clientId":"c1","answer":{"key":"A"}}]}""",
            body,
        )
    }

    @Test
    fun started_session_carries_the_activity_policy() {
        val dto =
            json.decodeFromString<StartedSessionDto>(
                """
                {"sessionId":"s1","deadline":10,"serverTime":9,
                 "policy":{"violationLimit":5,"focusThresholdMs":12000,
                 "snapshotIntervalMs":30000,"heartbeatIntervalMs":15000,
                 "gapRecordedMs":300000,"gapTokenMs":1800000,
                 "gapViolationFrom":3,"tokenSlotSeconds":600}}
                """.trimIndent(),
            )

        assertEquals(5, dto.policy.violationLimit)
        assertEquals(12_000L, dto.policy.focusThresholdMs)
        assertEquals(30_000L, dto.policy.snapshotIntervalMs)
        assertEquals(3, dto.policy.gapViolationFrom)
    }

    @Test
    fun started_session_without_a_policy_falls_back_to_the_safe_defaults() {
        val dto =
            json.decodeFromString<StartedSessionDto>(
                """{"sessionId":"s1","deadline":10,"serverTime":9}""",
            )

        assertEquals(3, dto.policy.violationLimit)
        assertEquals(10_000L, dto.policy.focusThresholdMs)
        assertEquals(15_000L, dto.policy.snapshotIntervalMs)
    }
}

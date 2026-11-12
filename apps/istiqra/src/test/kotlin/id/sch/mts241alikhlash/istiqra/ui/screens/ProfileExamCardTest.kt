package id.sch.mts241alikhlash.istiqra.ui.screens

import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.myExam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileExamCardTest {
    private fun exam(
        lifecycle: String,
        roomName: String? = null,
        seatNumber: Int? = null,
        participantNumber: String? = null,
        eventName: String? = null,
    ) = myExam(
        id = "e1",
        title = "UTS - IPA",
        subjectId = "s",
        startsAt = "2026-11-09T01:00:00.000Z",
        endsAt = "2026-11-09T03:00:00.000Z",
        durationMinutes = 90,
        status = MyExamDto.Status.OPEN,
        lifecycle = MyExamDto.Lifecycle.valueOf(lifecycle),
        roomName = roomName,
        seatNumber = seatNumber,
        participantNumber = participantNumber,
        eventName = eventName,
    )

    @Test
    fun `an open exam shows the room, participant and seat`() {
        val rows =
            profileExamRows(
                exam(
                    lifecycle = "OPEN",
                    roomName = "Ruang 3",
                    seatNumber = 12,
                    participantNumber = "2024-001",
                    eventName = "UTS Ganjil",
                ),
            )

        assertEquals(
            listOf(
                "Ujian" to "UTS Ganjil",
                "Ruangan" to "Ruang 3",
                "No. peserta" to "2024-001",
                "No. meja" to "12",
            ),
            rows,
        )
    }

    @Test
    fun `an open exam without a room still shows the participant and says it is unset`() {
        val rows = profileExamRows(exam(lifecycle = "OPEN", participantNumber = "2024-001"))

        assertTrue(("Ruangan" to "Belum ditetapkan") in rows)
        assertTrue(("No. peserta" to "2024-001") in rows)
        assertTrue(("No. meja" to "Belum ditetapkan") in rows)
    }

    @Test
    fun `the earliest open exam wins the profile card`() {
        val first = exam(lifecycle = "OPEN").copy(id = "e1", startsAt = "2026-11-09T01:00:00.000Z")
        val second = exam(lifecycle = "OPEN").copy(id = "e2", startsAt = "2026-11-10T01:00:00.000Z")

        assertEquals(first, profileCardExam(listOf(second, first)))
        assertEquals(null, profileCardExam(listOf(exam(lifecycle = "FINISHED"))))
    }

    @Test
    fun `a room without a participant number drops only the participant row`() {
        val rows = profileExamRows(exam(lifecycle = "OPEN", roomName = "Ruang 3"))

        assertEquals(listOf("Ruangan" to "Ruang 3", "No. meja" to "Belum ditetapkan"), rows)
    }

    @Test
    fun `a finished exam shows nothing`() {
        assertEquals(emptyList<Pair<String, String>>(), profileExamRows(exam(lifecycle = "FINISHED")))
        assertEquals(emptyList<Pair<String, String>>(), profileExamRows(exam(lifecycle = "UPCOMING")))
    }

    @Test
    fun `no exam shows nothing`() {
        assertEquals(emptyList<Pair<String, String>>(), profileExamRows(null))
    }
}

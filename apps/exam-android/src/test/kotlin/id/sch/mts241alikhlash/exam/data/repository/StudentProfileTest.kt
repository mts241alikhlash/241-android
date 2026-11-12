package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.api.student.models.MyClassroomResponseClassroomAcademicYearDto
import id.sch.mts241alikhlash.exam.api.student.models.MyClassroomResponseClassroomDto
import id.sch.mts241alikhlash.exam.api.student.models.MyClassroomResponseDto
import id.sch.mts241alikhlash.exam.api.student.models.MyClassroomResponseSemesterDto
import id.sch.mts241alikhlash.exam.api.student.models.StudentResponseDto
import id.sch.mts241alikhlash.exam.api.student.models.StudentResponseGradeDto
import id.sch.mts241alikhlash.exam.api.student.models.StudentResponseUserDto
import id.sch.mts241alikhlash.exam.api.student.models.StudentResponseUserProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentProfileTest {
    private val me =
        StudentResponseDto(
            id = "s1",
            userId = "u1",
            nis = "2024001",
            nisn = "0012345601",
            status = StudentResponseDto.Status.ACTIVE,
            grade = StudentResponseGradeDto(id = "g1", name = "VII", level = 7),
            user =
                StudentResponseUserDto(
                    id = "u1",
                    identifier = "2024001",
                    isActive = true,
                    profile =
                        StudentResponseUserProfileDto(
                            name = "Ahmad Fauzi",
                            gender = "MALE",
                            birthPlace = "Bandung",
                            birthDate = "2013-03-14T00:00:00.000Z",
                            email = "ahmad@example.com",
                            phone = "081200000001",
                        ),
                ),
        )

    private val classroom =
        MyClassroomResponseDto(
            classroom =
                MyClassroomResponseClassroomDto(
                    id = "c1",
                    academicYearId = "ay1",
                    capacity = 30,
                    code = "7A",
                    displayName = "7A (VII A)",
                    gradeId = "g1",
                    name = "7A",
                    academicYear = MyClassroomResponseClassroomAcademicYearDto(id = "ay1", isActive = true, name = "2026/2027"),
                ),
            semester = MyClassroomResponseSemesterDto(id = "sem1", sequence = 1, isActive = true, name = "Ganjil"),
            classmates = emptyList(),
            subjects = emptyList(),
            structure = null,
            supervisor = null,
        )

    @Test
    fun `profile maps the student and classroom fields`() {
        val profile = studentProfileOf(me, classroom, "fallback")

        assertEquals("Ahmad Fauzi", profile.name)
        assertEquals("2024001", profile.nis)
        assertEquals("2024001", profile.username)
        assertEquals("Laki-laki", profile.gender)
        assertEquals("Bandung", profile.birthPlace)
        assertEquals("14 Maret 2013", profile.birthDate)
        assertEquals("Aktif", profile.status)
        assertEquals("VII", profile.gradeName)
        assertEquals("7A (VII A)", profile.classLabel)
        assertEquals("2026/2027", profile.academicYear)
        assertEquals("Ganjil", profile.semester)
    }

    @Test
    fun `profile without a classroom keeps the grade as the class label`() {
        val profile = studentProfileOf(me, null, "fallback")

        assertEquals("VII", profile.classLabel)
        assertNull(profile.academicYear)
        assertNull(profile.semester)
    }

    @Test
    fun `profile falls back to the session name and hides blank fields`() {
        val bare =
            StudentResponseDto(
                id = "s2",
                userId = "u2",
                nis = "2024002",
                nisn = "",
                status = StudentResponseDto.Status.ACTIVE,
                user =
                    StudentResponseUserDto(
                        id = "u2",
                        identifier = "",
                        isActive = true,
                        profile = StudentResponseUserProfileDto(name = " ", email = "", phone = ""),
                    ),
            )

        val profile = studentProfileOf(bare, null, "Siti Aisyah")

        assertEquals("Siti Aisyah", profile.name)
        assertNull(profile.gender)
        assertNull(profile.email)
        assertNull(profile.phone)
        assertEquals("Aktif", profile.status)
        assertNull(profile.username)
    }

    @Test
    fun `initials take the first letters of up to two words`() {
        assertEquals("AF", studentProfileOf(me, null, "x").initials)
        assertEquals("SA", studentProfileOf(me.copy(user = me.user.copy(profile = null)), null, "siti aisyah rahma").initials)
        assertEquals("?", studentProfileOf(me.copy(user = me.user.copy(profile = null)), null, "  ").initials)
    }

    @Test
    fun `dates read in Indonesian and bad input gives null`() {
        assertEquals("1 Januari 2012", indonesianDate("2012-01-01"))
        assertEquals("17 Agustus 2011", indonesianDate("2011-08-17T00:00:00.000Z"))
        assertNull(indonesianDate("kemarin"))
    }
}

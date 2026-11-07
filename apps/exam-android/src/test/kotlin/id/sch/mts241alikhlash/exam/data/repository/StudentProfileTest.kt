package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.data.model.MyClassroomDto
import id.sch.mts241alikhlash.exam.data.model.MyClassroomInfoDto
import id.sch.mts241alikhlash.exam.data.model.NamedDto
import id.sch.mts241alikhlash.exam.data.model.SemesterDto
import id.sch.mts241alikhlash.exam.data.model.StudentGradeDto
import id.sch.mts241alikhlash.exam.data.model.StudentMeDto
import id.sch.mts241alikhlash.exam.data.model.StudentPersonDto
import id.sch.mts241alikhlash.exam.data.model.StudentUserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentProfileTest {
    private val me =
        StudentMeDto(
            id = "s1",
            nis = "2024001",
            nisn = "0012345601",
            status = "ACTIVE",
            grade = StudentGradeDto(name = "VII", level = 7),
            user =
                StudentUserDto(
                    identifier = "2024001",
                    profile =
                        StudentPersonDto(
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
        MyClassroomDto(
            classroom =
                MyClassroomInfoDto(
                    code = "7A",
                    displayName = "7A (VII A)",
                    academicYear = NamedDto("2026/2027"),
                ),
            semester = SemesterDto("Ganjil"),
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
        val bare = StudentMeDto(id = "s2", nis = "2024002", status = "", user = StudentUserDto(profile = StudentPersonDto(name = " ", email = "", phone = "")))

        val profile = studentProfileOf(bare, null, "Siti Aisyah")

        assertEquals("Siti Aisyah", profile.name)
        assertNull(profile.gender)
        assertNull(profile.email)
        assertNull(profile.phone)
        assertNull(profile.status)
        assertNull(profile.username)
    }

    @Test
    fun `initials take the first letters of up to two words`() {
        assertEquals("AF", studentProfileOf(me, null, "x").initials)
        assertEquals("SA", studentProfileOf(me.copy(user = null), null, "siti aisyah rahma").initials)
        assertEquals("?", studentProfileOf(me.copy(user = null), null, "  ").initials)
    }

    @Test
    fun `dates read in Indonesian and bad input gives null`() {
        assertEquals("1 Januari 2012", indonesianDate("2012-01-01"))
        assertEquals("17 Agustus 2011", indonesianDate("2011-08-17T00:00:00.000Z"))
        assertNull(indonesianDate("kemarin"))
    }
}

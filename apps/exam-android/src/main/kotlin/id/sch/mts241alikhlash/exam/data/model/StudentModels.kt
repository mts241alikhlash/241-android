package id.sch.mts241alikhlash.exam.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentMeDto(
    val id: String,
    val nis: String,
    val nisn: String = "",
    val status: String = "",
    val grade: StudentGradeDto? = null,
    val user: StudentUserDto? = null,
)

@Serializable
data class StudentGradeDto(
    val name: String = "",
    val level: Int = 0,
)

@Serializable
data class StudentUserDto(
    val identifier: String? = null,
    val profile: StudentPersonDto? = null,
)

@Serializable
data class StudentPersonDto(
    val name: String? = null,
    val gender: String? = null,
    val birthPlace: String? = null,
    val birthDate: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
data class MyClassroomDto(
    val classroom: MyClassroomInfoDto? = null,
    val semester: SemesterDto? = null,
)

@Serializable
data class SemesterDto(
    val name: String? = null,
)

@Serializable
data class MyClassroomInfoDto(
    val code: String = "",
    val displayName: String = "",
    val academicYear: NamedDto? = null,
)

@Serializable
data class NamedDto(
    val name: String = "",
)

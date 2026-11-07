package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.data.model.MyClassroomDto
import id.sch.mts241alikhlash.exam.data.model.StudentMeDto
import id.sch.mts241alikhlash.exam.data.remote.StudentApiService
import id.sch.mts241alikhlash.exam.data.remote.guarded
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class StudentProfile(
    val name: String,
    val nis: String,
    val nisn: String,
    val gender: String?,
    val birthPlace: String?,
    val birthDate: String?,
    val email: String?,
    val phone: String?,
    val gradeName: String?,
    val classroomName: String?,
    val academicYear: String?,
    val status: String? = null,
    val username: String? = null,
    val semester: String? = null,
) {
    val initials: String
        get() =
            name
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifEmpty { "?" }

    val classLabel: String? get() = classroomName ?: gradeName
}

fun studentProfileOf(
    me: StudentMeDto,
    classroom: MyClassroomDto?,
    fallbackName: String,
): StudentProfile {
    val person = me.user?.profile
    val info = classroom?.classroom
    return StudentProfile(
        name = person?.name?.takeIf { it.isNotBlank() } ?: fallbackName,
        nis = me.nis,
        nisn = me.nisn,
        gender =
            when (person?.gender) {
                "MALE" -> "Laki-laki"
                "FEMALE" -> "Perempuan"
                else -> null
            },
        birthPlace = person?.birthPlace?.takeIf { it.isNotBlank() },
        birthDate = person?.birthDate?.let(::indonesianDate),
        email = person?.email?.takeIf { it.isNotBlank() },
        phone = person?.phone?.takeIf { it.isNotBlank() },
        gradeName = me.grade?.name?.takeIf { it.isNotBlank() },
        classroomName = info?.displayName?.takeIf { it.isNotBlank() },
        academicYear = info?.academicYear?.name?.takeIf { it.isNotBlank() },
        status =
            when (me.status) {
                "ACTIVE" -> "Aktif"
                "" -> null
                else -> me.status.lowercase().replaceFirstChar { it.uppercase() }
            },
        username = me.user?.identifier?.takeIf { it.isNotBlank() },
        semester = classroom?.semester?.name?.takeIf { it.isNotBlank() },
    )
}

fun indonesianDate(iso: String): String? =
    runCatching {
        val parsed =
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(iso.take(10)) ?: return null
        SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID"))
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(parsed)
    }.getOrNull()

class StudentProfileRepository(
    private val api: StudentApiService,
) {
    suspend fun load(fallbackName: String): StudentProfile {
        val me = guarded { api.me() }
        val classroom =
            try {
                guarded { api.classroom() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        return studentProfileOf(me, classroom, fallbackName)
    }
}

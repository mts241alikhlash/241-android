package id.sch.mts241alikhlash.istiqra.ui.screens

import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto

fun answerSyncLabel(summary: AnswerSyncSummary): String =
    if (summary.allConfirmed) {
        "Tersimpan: ${summary.totalAnswered} soal (Terkirim ke server: ${summary.confirmedCount})"
    } else {
        "Tersimpan di HP: ${summary.totalAnswered} soal (Menunggu koneksi: ${summary.pendingCount})"
    }

fun examStatusSummary(exams: List<MyExamDto>): String {
    val running = exams.filter { it.status == MyExamDto.Status.IN_PROGRESS }
    if (running.isNotEmpty()) return "Ujian berlangsung · ${running.first().title}"
    val open = exams.filter { it.status == MyExamDto.Status.OPEN }
    if (open.size > 1) return "${open.size} ujian sedang dibuka"
    if (open.size == 1) return "Ujian dibuka · ${open.first().title}"
    val next = exams.filter { it.status == MyExamDto.Status.UPCOMING }.minByOrNull { it.startsAt }
    if (next != null) return "Berikutnya · ${next.title}"
    return "Tidak ada ujian"
}

fun semesterLabel(
    semester: String?,
    academicYear: String?,
): String? = listOfNotNull(semester, academicYear).joinToString(" · ").ifBlank { null }

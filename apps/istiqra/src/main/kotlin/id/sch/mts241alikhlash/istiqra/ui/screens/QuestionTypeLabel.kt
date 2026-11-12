package id.sch.mts241alikhlash.istiqra.ui.screens

import java.util.Locale

fun questionTypeLabel(type: String): String {
    val normalized = type.trim()
    if (normalized.isEmpty()) return "Soal"
    return when (normalized.lowercase(Locale.ROOT)) {
        "multiple_choice" -> "Pilihan Ganda"
        "multiple_response" -> "Pilihan Ganda Kompleks"
        "true_false" -> "Benar / Salah"
        "essay" -> "Uraian"
        "short_answer" -> "Jawaban Singkat"
        else -> normalized.replace("_", " ").replaceFirstChar { it.uppercase(Locale.ROOT) }
    }
}

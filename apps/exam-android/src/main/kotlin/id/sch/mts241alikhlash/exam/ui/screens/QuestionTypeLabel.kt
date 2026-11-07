package id.sch.mts241alikhlash.exam.ui.screens

import java.util.Locale

/**
 * Human-readable Indonesian label for a question type.
 *
 * Known types map to their Indonesian names; unknown non-blank types fall back to a
 * readable uppercase form with underscores replaced by spaces; blank types get a
 * generic "SOAL" label.
 *
 * Case conversion pins [Locale.ROOT]: the wire values are ASCII identifiers, not
 * user-facing prose. Under a Turkish default locale `"multiple_choice".lowercase()`
 * yields `"multıple_choıce"` (dotless ı), which misses every mapping; `uppercase()`
 * likewise turns `i` into `İ`. Neither result should depend on the device language.
 */
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

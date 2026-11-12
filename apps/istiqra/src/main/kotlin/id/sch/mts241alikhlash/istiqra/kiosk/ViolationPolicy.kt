package id.sch.mts241alikhlash.istiqra.kiosk

import id.sch.mts241alikhlash.istiqra.data.session.ExamRuntimePolicy

object ViolationPolicy {
    const val LIMIT = 3

    private val COUNTED = setOf("left_app", "pin_lost", "screen_share_detected")

    fun counts(reason: String): Boolean = reason in COUNTED

    fun currentLimit(): Int = ExamRuntimePolicy.current.violationLimit

    fun exitWarning(): String = warning(currentLimit() + 1, currentLimit())

    fun exceeded(
        count: Int,
        limit: Int = LIMIT,
    ): Boolean = count > limit

    fun warning(
        count: Int,
        limit: Int = LIMIT,
    ): String =
        when {
            count > limit -> "Ujian dikumpulkan karena keluar dari aplikasi lebih dari $limit kali."
            count == limit ->
                "Peringatan terakhir ($count dari $limit): keluar sekali lagi dan ujian langsung dikumpulkan."
            else -> "Peringatan $count dari $limit: dilarang keluar dari aplikasi ujian."
        }
}

package id.sch.mts241alikhlash.exam.kiosk

object ViolationPolicy {
    const val LIMIT = 3

    private val COUNTED = setOf("left_app", "pin_lost", "screen_share_detected")

    fun counts(reason: String): Boolean = reason in COUNTED

    fun exceeded(count: Int): Boolean = count > LIMIT

    fun warning(count: Int): String =
        when {
            count > LIMIT -> "Ujian dikumpulkan karena keluar dari aplikasi lebih dari $LIMIT kali."
            count == LIMIT ->
                "Peringatan terakhir ($count dari $LIMIT): keluar sekali lagi dan ujian langsung dikumpulkan."
            else -> "Peringatan $count dari $LIMIT: dilarang keluar dari aplikasi ujian."
        }
}

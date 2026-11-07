package id.sch.mts241alikhlash.exam.data.session

import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val ISO_PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX")

internal fun parseIsoMillis(iso: String): Long {
    var failure: Exception? = null
    for (pattern in ISO_PATTERNS) {
        try {
            return SimpleDateFormat(pattern, Locale.ROOT)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(iso)!!
                .time
        } catch (error: Exception) {
            failure = error
        }
    }
    throw IllegalArgumentException("Unparseable server time: $iso", failure)
}

class ServerClock(
    private val elapsedRealtimeProvider: () -> Long = SystemClock::elapsedRealtime,
) {
    @Volatile
    private var baseServerMillis: Long? = null

    @Volatile
    private var baseElapsedMillis: Long = 0L

    fun sync(serverIsoTime: String) = syncEpochMillis(parseIsoMillis(serverIsoTime))

    fun syncEpochMillis(serverEpochMillis: Long) {
        baseElapsedMillis = elapsedRealtimeProvider()
        baseServerMillis = serverEpochMillis
    }

    fun nowMillis(): Long {
        val base = baseServerMillis ?: return System.currentTimeMillis()
        return base + (elapsedRealtimeProvider() - baseElapsedMillis)
    }
}

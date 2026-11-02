package id.sch.mts241alikhlash.cbt.kiosk

import android.content.Context
import androidx.core.content.edit

class ViolationStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("exam_violations", Context.MODE_PRIVATE)

    fun count(): Int = prefs.getInt(KEY_COUNT, 0)

    fun counted(): Int =
        entries().count { ViolationPolicy.counts(it.substringAfter('|')) } +
            prefs.getInt(KEY_BASELINE, 0)

    fun restore(serverCount: Int) {
        val gap = serverCount - counted()
        if (gap > 0) {
            prefs.edit { putInt(KEY_BASELINE, prefs.getInt(KEY_BASELINE, 0) + gap) }
        }
    }

    fun beginSession(sessionId: String) {
        if (prefs.getString(KEY_SESSION, null) == sessionId) return
        prefs.edit {
            clear()
            putString(KEY_SESSION, sessionId)
        }
    }

    fun record(reason: String): Int {
        val total = count() + 1
        val entry = "${System.currentTimeMillis()}|$reason"
        val log = (listOf(entry) + entries()).take(MAX_ENTRIES)
        prefs.edit {
            putInt(KEY_COUNT, total)
            putString(KEY_LOG, log.joinToString("\n"))
        }
        return total
    }

    fun entries(): List<String> =
        prefs.getString(KEY_LOG, null)
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            .orEmpty()

    fun reset() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_COUNT = "count"
        const val KEY_BASELINE = "baseline"
        const val KEY_LOG = "log"
        const val KEY_SESSION = "session"
        const val MAX_ENTRIES = 200
    }
}

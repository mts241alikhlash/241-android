package id.sch.mts241alikhlash.cbt.kiosk

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.edit

class DndController(context: Context) {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)
    private val prefs = appContext.getSharedPreferences("exam_dnd", Context.MODE_PRIVATE)

    fun isAccessGranted(): Boolean = manager?.isNotificationPolicyAccessGranted == true

    fun openPolicySettings(activity: Activity, guard: InternalIntentGuard) {
        guard.markLaunchingInternalDialog()
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    fun enableExamDnd(): Boolean {
        val notifications = manager ?: return false
        if (!notifications.isNotificationPolicyAccessGranted) return false
        if (!prefs.contains(KEY_FILTER)) {
            val current = notifications.currentInterruptionFilter
            val baseline = if (current == NotificationManager.INTERRUPTION_FILTER_NONE) {
                NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                current
            }
            prefs.edit { putInt(KEY_FILTER, baseline) }
        }
        return runCatching {
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }.isSuccess
    }

    fun restorePreviousDnd() {
        val notifications = manager ?: return
        val previous = if (prefs.contains(KEY_FILTER)) {
            prefs.getInt(KEY_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        runCatching { notifications.setInterruptionFilter(previous) }
        prefs.edit { remove(KEY_FILTER) }
    }

    private companion object {
        const val KEY_FILTER = "previous_filter"
    }
}

package id.sch.mts241alikhlash.cbt.kiosk

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

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
            prefs.edit().putInt(KEY_FILTER, notifications.currentInterruptionFilter).apply()
        }
        return runCatching {
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }.isSuccess
    }

    fun restorePreviousDnd() {
        val notifications = manager ?: return
        if (!prefs.contains(KEY_FILTER)) return
        val previous = prefs.getInt(KEY_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        runCatching { notifications.setInterruptionFilter(previous) }
        prefs.edit().remove(KEY_FILTER).apply()
    }

    private companion object {
        const val KEY_FILTER = "previous_filter"
    }
}

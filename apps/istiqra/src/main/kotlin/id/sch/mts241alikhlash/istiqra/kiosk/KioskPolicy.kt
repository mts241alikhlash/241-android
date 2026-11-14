package id.sch.mts241alikhlash.istiqra.kiosk

import android.app.Activity
import android.app.ActivityManager

object KioskPolicy {
    fun isSecure(lockTaskModeState: Int): Boolean =
        lockTaskModeState == ActivityManager.LOCK_TASK_MODE_PINNED ||
            lockTaskModeState == ActivityManager.LOCK_TASK_MODE_LOCKED

    fun lockTaskState(activity: Activity): Int =
        activity.getSystemService(ActivityManager::class.java)?.lockTaskModeState
            ?: ActivityManager.LOCK_TASK_MODE_NONE

    fun isLocked(activity: Activity): Boolean = isSecure(lockTaskState(activity))
}

class KioskPinRetryPolicy {
    var pinAttempts: Int = 0
        private set

    var kioskMode: String = MODE_PINNED
        private set

    var gateOpen: Boolean = false
        private set

    fun allowUnpinned() {
        kioskMode = MODE_UNPINNED
        gateOpen = true
    }

    fun onRetryAttempt(locked: Boolean): Boolean {
        if (kioskMode == MODE_UNPINNED) {
            gateOpen = true
            return true
        }
        if (locked) {
            pinAttempts = 0
            gateOpen = true
            return true
        }
        pinAttempts += 1
        gateOpen = false
        return false
    }

    fun onStateRefresh(locked: Boolean) {
        if (locked) {
            pinAttempts = 0
        }
        gateOpen = locked || kioskMode == MODE_UNPINNED
    }

    fun reset() {
        pinAttempts = 0
        kioskMode = MODE_PINNED
        gateOpen = false
    }

    companion object {
        const val MODE_PINNED = "PINNED"
        const val MODE_UNPINNED = "UNPINNED"
    }
}

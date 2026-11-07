package id.sch.mts241alikhlash.exam.kiosk

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

class KioskPinRetryPolicy(
    private val requiredFailures: Int = REQUIRED_FAILURES,
) {
    var pinAttempts: Int = 0
        private set

    var canProceedUnpinned: Boolean = false
        private set

    var isUnpinned: Boolean = false
        private set

    var kioskMode: String = MODE_PINNED
        private set

    var gateOpen: Boolean = false
        private set

    fun onRetryAttempt(locked: Boolean): Boolean {
        if (locked) {
            pinAttempts = 0
            canProceedUnpinned = false
            gateOpen = true
            return true
        }
        pinAttempts = minOf(pinAttempts + 1, requiredFailures)
        canProceedUnpinned = pinAttempts >= requiredFailures
        gateOpen = isUnpinned
        return false
    }

    fun onStateRefresh(locked: Boolean) {
        if (locked) {
            pinAttempts = 0
            canProceedUnpinned = false
        }
        gateOpen = locked || isUnpinned
    }

    fun proceedUnpinned() {
        isUnpinned = true
        canProceedUnpinned = false
        kioskMode = MODE_STRICT_UNPINNED
        gateOpen = true
    }

    fun reset() {
        pinAttempts = 0
        canProceedUnpinned = false
        isUnpinned = false
        kioskMode = MODE_PINNED
        gateOpen = false
    }

    companion object {
        const val REQUIRED_FAILURES = 2
        const val MODE_PINNED = "PINNED"
        const val MODE_STRICT_UNPINNED = "STRICT_UNPINNED"
    }
}

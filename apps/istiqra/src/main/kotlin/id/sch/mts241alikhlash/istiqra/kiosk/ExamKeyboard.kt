package id.sch.mts241alikhlash.istiqra.kiosk

import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

fun isTrustedKeyboard(
    systemApp: Boolean,
    updatedSystemApp: Boolean,
): Boolean = systemApp || updatedSystemApp

fun isCurrentKeyboardTrusted(context: Context): Boolean {
    val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return true
    val manager = context.getSystemService(InputMethodManager::class.java) ?: return true
    val info = manager.enabledInputMethodList.firstOrNull { it.id == current } ?: return true
    val flags = info.serviceInfo.applicationInfo.flags
    return isTrustedKeyboard(
        systemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0,
        updatedSystemApp = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
    )
}

fun showKeyboardPicker(context: Context) {
    context.getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
}

fun clearExamClipboard(context: Context) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
        }
    }
}

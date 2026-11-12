package id.sch.mts241alikhlash.istiqra.kiosk

import android.view.KeyEvent

val EXAM_BLOCKED_KEYS =
    setOf(
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_BACK,
    )

fun blocksExamKey(
    isExamActive: Boolean,
    keyCode: Int,
): Boolean = isExamActive && keyCode in EXAM_BLOCKED_KEYS

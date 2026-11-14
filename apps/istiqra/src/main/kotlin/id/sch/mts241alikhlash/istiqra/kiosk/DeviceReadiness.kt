package id.sch.mts241alikhlash.istiqra.kiosk

data class DeviceReadinessState(
    val dndGranted: Boolean = false,
    val audioClear: Boolean = true,
    val online: Boolean = false,
    val keyboardTrusted: Boolean = true,
) {
    val allReady: Boolean get() = dndGranted && audioClear && online && keyboardTrusted

    fun blockingReasons(): List<ReadinessBlocker> =
        buildList {
            if (!dndGranted) add(ReadinessBlocker.DND)
            if (!audioClear) add(ReadinessBlocker.CALL_OR_MIC)
            if (!online) add(ReadinessBlocker.OFFLINE)
            if (!keyboardTrusted) add(ReadinessBlocker.KEYBOARD)
        }
}

enum class ReadinessBlocker { DND, CALL_OR_MIC, OFFLINE, KEYBOARD }

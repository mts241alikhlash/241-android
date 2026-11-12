package id.sch.mts241alikhlash.istiqra.kiosk

data class DeviceReadinessState(
    val dndGranted: Boolean = false,
    val audioClear: Boolean = true,
    val online: Boolean = false,
) {
    val allReady: Boolean get() = dndGranted && audioClear && online

    fun blockingReasons(): List<ReadinessBlocker> =
        buildList {
            if (!dndGranted) add(ReadinessBlocker.DND)
            if (!audioClear) add(ReadinessBlocker.CALL_OR_MIC)
            if (!online) add(ReadinessBlocker.OFFLINE)
        }
}

enum class ReadinessBlocker { DND, CALL_OR_MIC, OFFLINE }

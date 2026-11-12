package id.sch.mts241alikhlash.istiqra.kiosk

data class DeviceReadinessState(
    val dndGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val audioClear: Boolean = true,
    val online: Boolean = false,
) {
    val allReady: Boolean get() = dndGranted && cameraGranted && audioClear && online

    fun blockingReasons(): List<ReadinessBlocker> =
        buildList {
            if (!dndGranted) add(ReadinessBlocker.DND)
            if (!cameraGranted) add(ReadinessBlocker.CAMERA)
            if (!audioClear) add(ReadinessBlocker.CALL_OR_MIC)
            if (!online) add(ReadinessBlocker.OFFLINE)
        }
}

enum class ReadinessBlocker { DND, CAMERA, CALL_OR_MIC, OFFLINE }

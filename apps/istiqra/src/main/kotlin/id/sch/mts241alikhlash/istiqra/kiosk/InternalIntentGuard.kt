package id.sch.mts241alikhlash.istiqra.kiosk

class InternalIntentGuard {
    @Volatile
    private var armed = false

    val isGuarded: Boolean
        get() = armed

    fun markLaunchingInternalDialog() {
        armed = true
    }

    fun consumeOnResume(): Boolean {
        val wasArmed = armed
        armed = false
        return wasArmed
    }

    companion object {
        val shared = InternalIntentGuard()
    }
}

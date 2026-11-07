package id.sch.mts241alikhlash.exam.data.session

suspend fun endExamSession(
    sessionRepository: SessionRepository,
    sessionId: String?,
    clearQuestions: suspend (String) -> Unit,
): Boolean {
    if (sessionId == null) {
        sessionRepository.finish()
        return false
    }

    val current = sessionRepository.current()
    if (current != null && current != sessionId) return false
    sessionRepository.finishIfCurrent(sessionId)

    runCatching { clearQuestions(sessionId) }
    return true
}

package id.sch.mts241alikhlash.exam.data.session

import id.sch.mts241alikhlash.exam.data.model.ExamPolicyDto

object ExamRuntimePolicy {
    @Volatile
    var current: ExamPolicyDto = ExamPolicyDto()

    fun apply(policy: ExamPolicyDto) {
        current = policy
    }

    fun reset() {
        current = ExamPolicyDto()
    }
}

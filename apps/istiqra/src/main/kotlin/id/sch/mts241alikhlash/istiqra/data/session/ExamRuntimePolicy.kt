package id.sch.mts241alikhlash.istiqra.data.session

import id.sch.mts241alikhlash.istiqra.api.exam.models.ExamPolicyDto

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

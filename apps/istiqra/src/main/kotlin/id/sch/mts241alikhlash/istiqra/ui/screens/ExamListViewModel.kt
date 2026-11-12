package id.sch.mts241alikhlash.istiqra.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.data.remote.ApiException
import id.sch.mts241alikhlash.istiqra.data.remote.GENERIC_ERROR_MESSAGE
import id.sch.mts241alikhlash.istiqra.data.repository.ExamCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExamListUiState {
    data object Loading : ExamListUiState

    data object Empty : ExamListUiState

    data class Content(
        val exams: List<MyExamDto>,
        val staleMessage: String? = null,
    ) : ExamListUiState

    data class Error(
        val message: String,
        val unauthorized: Boolean,
    ) : ExamListUiState
}

fun examTypeLabel(type: String?): String? =
    when (type) {
        "DAILY" -> "Ulangan Harian"
        "ASSIGNMENT" -> "Tugas"
        "MIDTERM" -> "UTS"
        "FINAL" -> "UAS"
        else -> null
    }

fun MyExamDto.displayTitle(): String {
    val label = examTypeLabel(type.value)
    val subject = subjectName?.takeIf { it.isNotBlank() }
    return if (label != null && subject != null) "$label - $subject" else title
}

fun MyExamDto.isSelectable(): Boolean = lifecycle == MyExamDto.Lifecycle.OPEN

fun MyExamDto.isExpandable(): Boolean = lifecycle != MyExamDto.Lifecycle.FINISHED

fun examMenuExams(
    exams: List<MyExamDto>,
    examWork: Boolean,
): List<MyExamDto> = exams.filter { it.category == if (examWork) MyExamDto.Category.EXAM else MyExamDto.Category.NON_EXAM }

class ExamListViewModel(
    private val catalog: ExamCatalogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<ExamListUiState>(ExamListUiState.Loading)
    val state: StateFlow<ExamListUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = ExamListUiState.Loading
        load()
    }

    fun reload() {
        if (_state.value is ExamListUiState.Loading) return
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val previous = _state.value as? ExamListUiState.Content
            _state.value =
                try {
                    val exams = catalog.listExams()
                    if (exams.isEmpty()) ExamListUiState.Empty else ExamListUiState.Content(exams)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: ApiException) {
                    failed(previous, error.message, error.statusCode == 401)
                } catch (error: Exception) {
                    failed(previous, GENERIC_ERROR_MESSAGE, false)
                }
        }
    }

    private fun failed(
        previous: ExamListUiState.Content?,
        message: String,
        unauthorized: Boolean,
    ): ExamListUiState =
        if (previous != null && !unauthorized) {
            previous.copy(staleMessage = message)
        } else {
            ExamListUiState.Error(message, unauthorized)
        }
}

fun examStatusLabel(
    status: String,
    startsAt: String,
    zone: java.util.TimeZone = java.util.TimeZone.getDefault(),
): String =
    when (status) {
        "OPEN" -> "Dibuka · Mulai"
        "IN_PROGRESS" -> "Sedang berlangsung · Lanjutkan"
        "UPCOMING" -> openingTime(startsAt, zone)?.let { "Dibuka $it" } ?: "Belum dibuka"
        "FINISHED" -> "Selesai"
        else -> status
    }

private fun openingTime(
    startsAt: String,
    zone: java.util.TimeZone,
): String? =
    runCatching {
        val parsed =
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.ROOT).parse(startsAt)
                ?: return null
        java.text
            .SimpleDateFormat("EEE, d MMM yyyy HH.mm", java.util.Locale.forLanguageTag("id-ID"))
            .apply { timeZone = zone }
            .format(parsed)
    }.getOrNull()

data class ExamWindow(
    val date: String,
    val shortDate: String,
    val start: String,
    val end: String,
)

fun examWindow(
    startsAt: String,
    endsAt: String,
    zone: java.util.TimeZone = java.util.TimeZone.getDefault(),
): ExamWindow? =
    runCatching {
        val parse = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.ROOT)
        val start = parse.parse(startsAt) ?: return null
        val end = parse.parse(endsAt) ?: return null
        val day =
            java.text
                .SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.forLanguageTag("id-ID"))
                .apply { timeZone = zone }
        val short =
            java.text
                .SimpleDateFormat("d MMM yyyy", java.util.Locale.forLanguageTag("id-ID"))
                .apply { timeZone = zone }
        val clock = java.text.SimpleDateFormat("HH.mm", java.util.Locale.ROOT).apply { timeZone = zone }
        ExamWindow(day.format(start), short.format(start), clock.format(start), clock.format(end))
    }.getOrNull()

fun examOpensIn(
    startsAt: String,
    nowMillis: Long,
): String? =
    runCatching {
        val start =
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.ROOT).parse(startsAt)
                ?: return null
        val minutes = (start.time - nowMillis) / 60_000
        when {
            minutes < 1 -> "kurang dari 1 menit"
            minutes < 60 -> "$minutes menit"
            minutes < 24 * 60 ->
                "${minutes / 60} jam".let { hours ->
                    if (minutes % 60 == 0L) hours else "$hours ${minutes % 60} menit"
                }
            else ->
                "${minutes / (24 * 60)} hari".let { days ->
                    val hours = (minutes % (24 * 60)) / 60
                    if (hours == 0L) days else "$days $hours jam"
                }
        }
    }.getOrNull()

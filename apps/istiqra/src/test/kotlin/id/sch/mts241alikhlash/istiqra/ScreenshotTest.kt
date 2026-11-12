package id.sch.mts241alikhlash.istiqra

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.istiqra.choices
import id.sch.mts241alikhlash.istiqra.data.auth.AuthUser
import id.sch.mts241alikhlash.istiqra.data.model.AnswerPayload
import id.sch.mts241alikhlash.istiqra.ui.AppRootState
import id.sch.mts241alikhlash.istiqra.ui.AssessmentAppRoot
import id.sch.mts241alikhlash.istiqra.ui.screens.ExamContent
import id.sch.mts241alikhlash.istiqra.ui.screens.ExamUiState
import id.sch.mts241alikhlash.istiqra.ui.screens.TokenScreen
import id.sch.mts241alikhlash.istiqra.ui.theme.ExamTheme
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
            theme = "android:Theme.Material.NoActionBar",
        )

    @Test
    fun tokenScreen() {
        paparazzi.snapshot {
            ExamTheme { TokenScreen(onStartExam = {}) }
        }
    }

    @Test
    fun appRootTokenScreen() {
        paparazzi.snapshot {
            AssessmentAppRoot(
                state =
                    AppRootState(
                        restoreComplete = true,
                        user = AuthUser("u1", "Ahmad", setOf("STUDENT")),
                        selectedExam =
                            myExam(
                                id = "e1",
                                title = "UTS Matematika",
                                subjectId = "Matematika",
                                startsAt = "2026-11-01T01:00:00Z",
                                endsAt = "2026-11-01T03:00:00Z",
                            ),
                        readinessComplete = true,
                    ),
                onRetryRestore = {},
                onStartExam = {},
                examContent = { _, _ -> },
            )
        }
    }

    @Test
    fun appRootRestoreError() {
        paparazzi.snapshot {
            AssessmentAppRoot(
                state =
                    AppRootState(
                        restoreComplete = true,
                        restoreError = "Gagal memulihkan sesi ujian. Coba lagi.",
                    ),
                onRetryRestore = {},
                onStartExam = {},
                examContent = { _, _ -> },
            )
        }
    }

    @Test
    fun examScreen() {
        val questions =
            listOf(
                PaperQuestionDto(
                    id = "q1",
                    number = 1,
                    type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
                    bodyHtml = "Hasil dari 12 x 8 adalah ...",
                    points = 1.0,
                    choices = choices("86", "96", "104", "112"),
                ),
                PaperQuestionDto(
                    id = "q2",
                    number = 2,
                    type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
                    bodyHtml = "Ibu kota Provinsi Jawa Timur adalah ...",
                    points = 1.0,
                    choices = choices("Malang", "Surabaya", "Kediri", "Madiun"),
                ),
                PaperQuestionDto(
                    id = "q3",
                    number = 3,
                    type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
                    bodyHtml = "Kalimat yang menggunakan ejaan baku adalah ...",
                    points = 1.0,
                    choices = choices("A", "B", "C", "D"),
                ),
            )
        val state =
            ExamUiState(
                isLoading = false,
                questions = questions,
                currentIndex = 0,
                answers = mapOf("q1" to AnswerPayload.Choice("B")),
                doubtfuls = setOf("q2"),
                remainingSeconds = 5340,
            )
        paparazzi.snapshot {
            ExamTheme {
                ExamContent(
                    state = state,
                    onAnswer = { _, _ -> },
                    onToggleDoubtful = {},
                    onGoToIndex = {},
                    onSubmit = {},
                    onRetry = {},
                    onFinish = {},
                )
            }
        }
    }

    @Test
    fun examErrorScreenHasSupervisorExit() {
        paparazzi.snapshot {
            ExamTheme {
                ExamContent(
                    state =
                        ExamUiState(
                            isLoading = false,
                            errorMessage = "Tidak dapat memuat soal. Periksa koneksi.",
                        ),
                    onAnswer = { _, _ -> },
                    onToggleDoubtful = {},
                    onGoToIndex = {},
                    onSubmit = {},
                    onRetry = {},
                    onFinish = {},
                    onSupervisorExit = {},
                )
            }
        }
    }

    @Test
    fun examScreenNarrow() {
        paparazzi.unsafeUpdateConfig(deviceConfig = NarrowDeviceConfig)
        paparazzi.snapshot { PreviewExamScreen() }
    }

    @Test
    fun examScreenNarrowLargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = NarrowLargeFontDeviceConfig)
        paparazzi.snapshot { PreviewExamScreen() }
    }

    @Test
    fun examScreenResumed() {
        val state =
            ExamUiState(
                isLoading = false,
                questions =
                    listOf(
                        PaperQuestionDto(
                            id = "q1",
                            number = 1,
                            type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
                            bodyHtml = "Hasil dari 12 x 8 adalah ...",
                            points = 1.0,
                            choices = choices("86", "96", "104", "112"),
                        ),
                    ),
                remainingSeconds = 2700,
                isResumed = true,
            )
        paparazzi.snapshot {
            ExamTheme {
                ExamContent(
                    state = state,
                    onAnswer = { _, _ -> },
                    onToggleDoubtful = {},
                    onGoToIndex = {},
                    onSubmit = {},
                    onRetry = {},
                    onFinish = {},
                )
            }
        }
    }
}

private val NarrowDeviceConfig =
    DeviceConfig.PIXEL_5.copy(
        screenWidth = 320,
        screenHeight = 640,
        xdpi = 160,
        ydpi = 160,
        density = com.android.resources.Density.MEDIUM,
    )

private val NarrowLargeFontDeviceConfig = NarrowDeviceConfig.copy(fontScale = 1.5f)

@Composable
private fun PreviewExamScreen() {
    val state =
        ExamUiState(
            isLoading = false,
            questions =
                listOf(
                    PaperQuestionDto(
                        id = "q1",
                        number = 1,
                        type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
                        bodyHtml = "Hasil dari 12 x 8 adalah ...",
                        points = 1.0,
                        choices = choices("86", "96", "104", "112"),
                    ),
                    PaperQuestionDto(
                        id = "q2",
                        number = 2,
                        type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
                        bodyHtml = "Ibu kota Provinsi Jawa Timur adalah ...",
                        points = 1.0,
                        choices = choices("Malang", "Surabaya", "Kediri", "Madiun"),
                    ),
                    PaperQuestionDto(
                        id = "q3",
                        number = 3,
                        type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
                        bodyHtml = "Kalimat yang menggunakan ejaan baku adalah ...",
                        points = 1.0,
                        choices = choices("A", "B", "C", "D"),
                    ),
                ),
            currentIndex = 0,
            answers = mapOf("q1" to AnswerPayload.Choice("B")),
            doubtfuls = setOf("q2"),
            remainingSeconds = 5340,
        )
    ExamTheme {
        ExamContent(
            state = state,
            onAnswer = { _, _ -> },
            onToggleDoubtful = {},
            onGoToIndex = {},
            onSubmit = {},
            onRetry = {},
            onFinish = {},
        )
    }
}

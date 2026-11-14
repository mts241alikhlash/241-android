package id.sch.mts241alikhlash.istiqra.ui.exam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import id.sch.mts241alikhlash.istiqra.ui.theme.ExamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnswerFocusTest {
    @get:Rule
    val rule = createComposeRule()

    private fun editable() = rule.onNode(hasSetTextAction())

    @Test
    fun tappingTheAnswerAgainAfterItWasDisabledDoesNotCrash() {
        var enabled by mutableStateOf(true)
        var text by mutableStateOf("")
        rule.setContent {
            ExamTheme {
                Box(Modifier.onFocusChanged { }) {
                    TextAnswerInput(value = text, multiline = true, enabled = enabled, onChange = { text = it })
                }
            }
        }
        editable().performClick()
        editable().performTextInput("abc")
        rule.runOnIdle { enabled = false }
        rule.runOnIdle { enabled = true }
        editable().performClick()
        rule.onAllNodesWithText("Jawaban Anda").onFirst().performClick()
        editable().performClick()
    }

    @Test
    fun tappingTheAnswerOfTheNextQuestionDoesNotCrash() {
        var question by mutableIntStateOf(0)
        var texts by mutableStateOf(mapOf<Int, String>())
        rule.setContent {
            ExamTheme {
                Column {
                    Box(Modifier.onFocusChanged { }) {
                        TextAnswerInput(
                            value = texts[question].orEmpty(),
                            multiline = question % 2 == 0,
                            enabled = true,
                            onChange = { texts = texts + (question to it) },
                        )
                    }
                }
            }
        }
        repeat(4) {
            editable().performClick()
            editable().performTextInput("abc")
            rule.runOnIdle { question += 1 }
        }
        editable().performClick()
    }

    @Test
    fun tappingTheAnswerAfterTheExamWasCoveredDoesNotCrash() {
        var covered by mutableStateOf(false)
        var text by mutableStateOf("")
        rule.setContent {
            ExamTheme {
                if (!covered) {
                    Box(Modifier.onFocusChanged { }) {
                        TextAnswerInput(value = text, multiline = true, enabled = true, onChange = { text = it })
                    }
                }
            }
        }
        editable().performClick()
        rule.runOnIdle { covered = true }
        rule.runOnIdle { covered = false }
        editable().performClick()
        rule.onAllNodesWithText("Jawaban Anda").onFirst().performClick()
    }

    @Test
    fun tappingTheAnswerInsideTheQuestionListAfterSwitchingDoesNotCrash() {
        var question by mutableIntStateOf(0)
        var texts by mutableStateOf(mapOf<Int, String>())
        rule.setContent {
            ExamTheme {
                LazyColumn(Modifier.height(400.dp)) {
                    item {
                        Column {
                            Text("Soal ${question + 1}")
                            Box(Modifier.onFocusChanged { }) {
                                if (question % 3 == 1) {
                                    Text("Pilihan")
                                } else {
                                    TextAnswerInput(
                                        value = texts[question].orEmpty(),
                                        multiline = question % 3 == 0,
                                        enabled = true,
                                        onChange = { texts = texts + (question to it) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        repeat(6) {
            if (question % 3 != 1) {
                editable().performClick()
                editable().performTextInput("abc")
            }
            rule.runOnIdle { question += 1 }
        }
        editable().performClick()
        rule.onAllNodesWithText("Jawaban Anda").onFirst().performClick()
    }
}

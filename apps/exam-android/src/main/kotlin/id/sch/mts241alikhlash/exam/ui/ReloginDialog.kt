package id.sch.mts241alikhlash.exam.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.ui.components.ActionButton
import id.sch.mts241alikhlash.exam.ui.components.ActionStyle
import id.sch.mts241alikhlash.exam.ui.components.AppDialog
import id.sch.mts241alikhlash.exam.ui.components.FloatingTextField
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens
import kotlinx.coroutines.launch

@Composable
fun ReloginDialog(
    onSubmit: suspend (identifier: String, password: String) -> String?,
    onDismiss: () -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AppDialog(
        title = "Masuk ulang",
        description = "Sesi login berakhir. Jawaban Anda tetap tersimpan.",
        onDismiss = { if (!checking) onDismiss() },
        dismissible = !checking,
        footerStart = {
            ActionButton(text = "Nanti", onClick = onDismiss, style = ActionStyle.Outline, enabled = !checking)
        },
        footerEnd = {
            ActionButton(
                text = "Masuk",
                loading = checking,
                onClick = {
                    checking = true
                    scope.launch {
                        message = onSubmit(identifier, password)
                        checking = false
                    }
                },
            )
        },
    ) {
        Text(
            text = "Masuk ulang dengan akun yang sama agar jawaban bisa dikirim.",
            fontFamily = DMSansFontFamily,
            fontSize = 13.sp,
            color = WebTokens.MutedForeground,
        )
        Spacer(Modifier.height(4.dp))
        FloatingTextField(
            value = identifier,
            onValueChange = {
                identifier = it
                message = null
            },
            label = "NIS",
            enabled = !checking,
            labelBackground = WebTokens.Background,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        FloatingTextField(
            value = password,
            onValueChange = {
                password = it
                message = null
            },
            label = "Kata sandi",
            enabled = !checking,
            password = true,
            error = message,
            labelBackground = WebTokens.Background,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
    }
}

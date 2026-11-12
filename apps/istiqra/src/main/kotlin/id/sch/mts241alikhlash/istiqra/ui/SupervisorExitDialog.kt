package id.sch.mts241alikhlash.istiqra.ui

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
import id.sch.mts241alikhlash.istiqra.data.auth.SupervisorResult
import id.sch.mts241alikhlash.istiqra.ui.components.ActionButton
import id.sch.mts241alikhlash.istiqra.ui.components.ActionStyle
import id.sch.mts241alikhlash.istiqra.ui.components.AppDialog
import id.sch.mts241alikhlash.istiqra.ui.components.FloatingTextField
import id.sch.mts241alikhlash.istiqra.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.istiqra.ui.theme.WebTokens
import kotlinx.coroutines.launch

@Composable
fun SupervisorExitDialog(
    onDismiss: () -> Unit,
    onVerify: suspend (identifier: String, password: String) -> SupervisorResult,
    onConfirm: () -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AppDialog(
        title = "Keluar darurat",
        onDismiss = { if (!checking) onDismiss() },
        dismissible = !checking,
        footerStart = {
            ActionButton(text = "Batal", onClick = onDismiss, style = ActionStyle.Outline, enabled = !checking)
        },
        footerEnd = {
            ActionButton(
                text = "Keluar",
                style = ActionStyle.Destructive,
                loading = checking,
                onClick = {
                    checking = true
                    scope.launch {
                        val result = onVerify(identifier, password)
                        checking = false
                        when (result) {
                            SupervisorResult.Approved -> onConfirm()
                            SupervisorResult.Denied -> message = "Akun bukan pengawas atau kata sandi salah"
                            SupervisorResult.Offline -> message = "Pengawas perlu koneksi untuk membuka kunci."
                        }
                    }
                },
            )
        },
    ) {
        Text(
            text = "Masuk dengan akun guru untuk melepas kunci perangkat.",
            fontFamily = DMSansFontFamily,
            fontSize = 13.sp,
            color = WebTokens.MutedForeground,
        )
        Spacer(Modifier.height(10.dp))
        FloatingTextField(
            value = identifier,
            onValueChange = {
                identifier = it
                message = null
            },
            label = "Akun guru",
            enabled = !checking,
            labelBackground = WebTokens.Background,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
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

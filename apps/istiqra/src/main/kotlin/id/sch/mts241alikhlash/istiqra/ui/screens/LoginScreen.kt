package id.sch.mts241alikhlash.istiqra.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.istiqra.BuildConfig
import id.sch.mts241alikhlash.istiqra.R
import id.sch.mts241alikhlash.istiqra.ui.components.BrandCardOverlap
import id.sch.mts241alikhlash.istiqra.ui.components.BrandEmblem
import id.sch.mts241alikhlash.istiqra.ui.components.BrandHero
import id.sch.mts241alikhlash.istiqra.ui.components.BrandWatermark
import id.sch.mts241alikhlash.istiqra.ui.components.FloatingTextField
import id.sch.mts241alikhlash.istiqra.ui.components.PrimaryButton
import id.sch.mts241alikhlash.istiqra.ui.components.pullUp
import id.sch.mts241alikhlash.istiqra.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.istiqra.ui.theme.WebTokens

private val HERO_MIN_HEIGHT = 232.dp
private const val HERO_SHARE = 0.36f

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(WebTokens.Background)) {
        val screenHeight = maxHeight
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val scrollState = rememberScrollState()
        LaunchedEffect(imeVisible) {
            if (!imeVisible) scrollState.scrollTo(0)
        }
        BrandWatermark(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = maxWidth * 0.28f, y = 110.dp)
                    .width(maxWidth * 1.1f)
                    .rotate(-24f),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(if (imeVisible) Modifier.verticalScroll(scrollState) else Modifier)
                    .heightIn(min = screenHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandHero(title = "Istiqra", tagline = "Kerjakan sendiri, banggakan sendiri", height = (screenHeight * HERO_SHARE).coerceAtLeast(HERO_MIN_HEIGHT))
                Column(
                    modifier =
                        Modifier
                            .pullUp(BrandCardOverlap)
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoginCard(
                        loading = state.loading,
                        error = state.error,
                        identifierError = state.identifierError,
                        passwordError = state.passwordError,
                        identifier = identifier,
                        onIdentifier = {
                            identifier = it
                            viewModel.clearFieldErrors()
                        },
                        password = password,
                        onPassword = {
                            password = it
                            viewModel.clearFieldErrors()
                        },
                        onSubmit = { viewModel.submit(identifier, password) },
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text =
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = WebTokens.Foreground)) {
                                    append("Kendala saat masuk? ")
                                }
                                append("Hubungi pengawas ujian.")
                            },
                        fontSize = 12.sp,
                        fontFamily = DMSansFontFamily,
                        color = WebTokens.MutedForeground,
                        maxLines = 1,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp, bottom = 20.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_name),
                    contentDescription = "Logo MTs 241 Al-Ikhlash",
                    modifier =
                        Modifier
                            .height(34.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Versi ${BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp,
                    fontFamily = DMSansFontFamily,
                    color = WebTokens.MutedForeground.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun LoginCard(
    loading: Boolean,
    error: String?,
    identifierError: String?,
    passwordError: String?,
    identifier: String,
    onIdentifier: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val shape = RoundedCornerShape(WebTokens.Radius + 2.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val radius = CornerRadius((WebTokens.Radius + 2.dp).toPx())
                    drawRoundRect(WebTokens.Card, cornerRadius = radius)
                    drawRoundRect(WebTokens.Border, cornerRadius = radius, style = Stroke(1.dp.toPx()))
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandEmblem()
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Masuk ke Ujian",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                letterSpacing = (-0.4).sp,
                color = WebTokens.Foreground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gunakan akun siswa yang terdaftar di MTs 241 Al-Ikhlash.",
                fontSize = 13.sp,
                fontFamily = DMSansFontFamily,
                color = WebTokens.MutedForeground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FloatingTextField(
                value = identifier,
                onValueChange = onIdentifier,
                label = "NIS",
                enabled = !loading,
                error = identifierError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(modifier = Modifier.height(4.dp))
            FloatingTextField(
                value = password,
                onValueChange = onPassword,
                label = "Kata sandi",
                enabled = !loading,
                password = true,
                error = passwordError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                    ),
            )
            error?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = WebTokens.Destructive,
                    fontSize = 12.sp,
                    fontFamily = DMSansFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryButton(
                text = "Masuk",
                onClick = onSubmit,
                enabled = !loading,
                loading = loading,
            )
        }
    }
}

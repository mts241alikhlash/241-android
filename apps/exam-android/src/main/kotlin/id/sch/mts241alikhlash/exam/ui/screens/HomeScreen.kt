package id.sch.mts241alikhlash.exam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.BuildConfig
import id.sch.mts241alikhlash.exam.R
import id.sch.mts241alikhlash.exam.data.model.ExamSummaryDto
import id.sch.mts241alikhlash.exam.ui.components.ActionButton
import id.sch.mts241alikhlash.exam.ui.components.ActionStyle
import id.sch.mts241alikhlash.exam.ui.components.AppDialog
import id.sch.mts241alikhlash.exam.ui.components.rememberIsOnline
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens
import kotlinx.coroutines.launch

private enum class HomeSection(
    val title: String,
    val icon: DrawerIcon,
) {
    Exams("Daftar Ujian", DrawerIcon.Exams),
    Schedule("Jadwal Ujian", DrawerIcon.Schedule),
    Profile("Profil Saya", DrawerIcon.Profile),
    Rules("Tata Tertib", DrawerIcon.Rules),
    Guide("Panduan Aplikasi", DrawerIcon.Guide),
}

private enum class DrawerIcon { Exams, Schedule, Profile, Rules, Guide, Logout }

@Composable
fun HomeScreen(
    userName: String,
    listViewModel: ExamListViewModel,
    profileViewModel: ProfileViewModel,
    notice: String?,
    onSelect: (ExamSummaryDto) -> Unit,
    onLogout: () -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(HomeSection.Exams) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val profileState by profileViewModel.state.collectAsState()
    val profile = (profileState as? ProfileUiState.Content)?.profile
    val listState by listViewModel.state.collectAsState()
    val participantNumber =
        (listState as? ExamListUiState.Content)
            ?.exams
            ?.firstOrNull { it.participantNumber != null && it.status != "FINISHED" }
            ?.participantNumber
    val identityFields =
        listOfNotNull(
            profile?.let { "NIS" to it.nis },
            profile?.classLabel?.let { "Kelas" to it },
            semesterLabel(profile?.semester, profile?.academicYear)?.let { "Semester" to it },
            participantNumber?.let { "No. peserta" to it },
        )
    val examStatus = (listState as? ExamListUiState.Content)?.exams?.let(::examStatusSummary)
    val online = rememberIsOnline()
    var confirmLogout by remember { mutableStateOf(false) }

    BackHandler(enabled = section != HomeSection.Exams && !drawerState.isOpen) {
        section = HomeSection.Exams
    }

    if (confirmLogout) {
        AppDialog(
            title = "Keluar dari akun?",
            onDismiss = { confirmLogout = false },
            footerStart = {
                ActionButton(text = "Batal", style = ActionStyle.Outline, onClick = { confirmLogout = false })
            },
            footerEnd = {
                ActionButton(
                    text = "Keluar",
                    style = ActionStyle.Destructive,
                    onClick = {
                        confirmLogout = false
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                )
            },
        ) {
            Text(
                text = "Anda perlu masuk lagi untuk memakai aplikasi.",
                fontSize = 13.sp,
                fontFamily = DMSansFontFamily,
                color = WebTokens.Foreground,
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerShape = RectangleShape,
                drawerContainerColor = WebTokens.Card,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(WebTokens.Foreground.copy(alpha = 0.04f), BlendMode.SrcIn),
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.BottomStart,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .width(300.dp)
                                .offset(x = (-70).dp, y = 60.dp)
                                .scale(1.3f),
                    )
                    Column(modifier = Modifier.fillMaxHeight()) {
                        DrawerHeader(
                            name = profile?.name ?: userName,
                            initials = profile?.initials ?: userName.take(1).uppercase().ifEmpty { "?" },
                            fields = identityFields,
                            status = examStatus,
                            online = online,
                        )
                        HomeSection.entries.forEach { entry ->
                            DrawerItem(
                                label = entry.title,
                                icon = entry.icon,
                                selected = section == entry,
                                onClick = {
                                    section = entry
                                    scope.launch { drawerState.close() }
                                },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
                        LogoutButton(
                            onClick = { confirmLogout = true },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_name),
                                contentDescription = "MTs 241 Al-Ikhlash",
                                modifier = Modifier.height(30.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "241-exam · Versi ${BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontFamily = DMSansFontFamily,
                                color = WebTokens.MutedForeground,
                            )
                        }
                    }
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize().background(WebTokens.Background)) {
            TopBar(
                title = section.title,
                onMenu = { scope.launch { drawerState.open() } },
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(WebTokens.Foreground.copy(alpha = 0.035f), BlendMode.SrcIn),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.BottomCenter,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .offset(y = 90.dp)
                            .scale(1.35f),
                )
                when (section) {
                    HomeSection.Exams ->
                        ExamListScreen(
                            viewModel = listViewModel,
                            onSelect = onSelect,
                            notice = notice,
                        )

                    HomeSection.Schedule -> ScheduleScreen(listViewModel)

                    HomeSection.Profile -> ProfileScreen(profileViewModel)

                    HomeSection.Rules ->
                        HelpScreen(
                            topics = examRules,
                        )

                    HomeSection.Guide ->
                        HelpScreen(
                            topics = appGuide,
                        )
                }
                if (section == HomeSection.Exams) {
                    RefreshButton(
                        onClick = listViewModel::reload,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onMenu: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(WebTokens.Card)
                .windowInsetsPadding(
                    WindowInsets.statusBars.union(WindowInsets.displayCutout),
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(WebTokens.Card)
                    .height(52.dp)
                    .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clickable(onClick = onMenu),
                contentAlignment = Alignment.Center,
            ) {
                MenuGlyph()
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                letterSpacing = (-0.2).sp,
                color = WebTokens.Foreground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo MTs 241 Al-Ikhlash",
                    modifier = Modifier.height(30.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
    }
}

@Composable
private fun RefreshButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .shadow(6.dp, CircleShape)
                .size(52.dp)
                .background(WebTokens.Primary, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val stroke = 2.2.dp.toPx()
            val inset = stroke
            drawArc(
                color = WebTokens.PrimaryForeground,
                startAngle = -50f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val head =
                Path().apply {
                    moveTo(size.width * 0.62f, size.height * 0.02f)
                    lineTo(size.width * 0.98f, size.height * 0.06f)
                    lineTo(size.width * 0.84f, size.height * 0.40f)
                    close()
                }
            drawPath(head, WebTokens.PrimaryForeground)
        }
    }
}

@Composable
private fun MenuGlyph() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val gap = size.height / 2f - stroke
        for (line in 0..2) {
            val y = stroke + line * gap
            drawLine(
                color = WebTokens.Foreground,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DrawerHeader(
    name: String,
    initials: String,
    fields: List<Pair<String, String>>,
    status: String?,
    online: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(WebTokens.Primary)
                .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(initials, size = 52)
            Spacer(Modifier.width(14.dp))
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                letterSpacing = (-0.2).sp,
                color = WebTokens.PrimaryForeground,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(top = 14.dp),
            )
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!online) {
                    Text(
                        text = "Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSansFontFamily,
                        color = WebTokens.PrimaryForeground.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (online) Color(0xFF6EE7A0) else Color(0xFFFCA5A5), CircleShape),
                )
            }
        }
        if (status != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSansFontFamily,
                color = WebTokens.PrimaryForeground,
                modifier =
                    Modifier
                        .background(WebTokens.PrimaryForeground.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        if (fields.isNotEmpty()) Spacer(Modifier.height(16.dp))
        fields.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontFamily = DMSansFontFamily,
                    color = WebTokens.PrimaryForeground.copy(alpha = 0.7f),
                    modifier = Modifier.width(96.dp),
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSansFontFamily,
                    color = WebTokens.PrimaryForeground,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(WebTokens.Radius)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(shape)
                .border(1.dp, WebTokens.Destructive.copy(alpha = 0.55f), shape)
                .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp)) { DrawerGlyph(DrawerIcon.Logout, WebTokens.Destructive) }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Keluar",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DMSansFontFamily,
            color = WebTokens.Destructive,
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: DrawerIcon,
    selected: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val color =
        when {
            destructive -> WebTokens.Destructive
            selected -> WebTokens.Primary
            else -> WebTokens.Foreground
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (selected) WebTokens.Primary.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onClick)
                .height(48.dp)
                .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawerGlyph(icon, color)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = DMSansFontFamily,
            color = color,
        )
    }
}

@Composable
private fun DrawerGlyph(
    icon: DrawerIcon,
    color: Color,
) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            DrawerIcon.Exams -> {
                drawRoundRect(
                    color,
                    topLeft = Offset(w * 0.14f, h * 0.08f),
                    size = Size(w * 0.72f, h * 0.84f),
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(w * 0.12f),
                    style = stroke,
                )
                for (line in 0..2) {
                    val y = h * (0.34f + line * 0.18f)
                    drawLine(color, Offset(w * 0.3f, y), Offset(w * 0.7f, y), stroke.width, StrokeCap.Round)
                }
            }

            DrawerIcon.Schedule -> {
                drawRoundRect(
                    color,
                    topLeft = Offset(w * 0.1f, h * 0.18f),
                    size = Size(w * 0.8f, h * 0.72f),
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(w * 0.12f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.1f, h * 0.42f), Offset(w * 0.9f, h * 0.42f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.32f, h * 0.06f), Offset(w * 0.32f, h * 0.28f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.68f, h * 0.06f), Offset(w * 0.68f, h * 0.28f), stroke.width, StrokeCap.Round)
            }

            DrawerIcon.Profile -> {
                drawCircle(color, radius = w * 0.2f, center = Offset(w * 0.5f, h * 0.3f), style = stroke)
                drawArc(
                    color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.14f, h * 0.58f),
                    size = Size(w * 0.72f, h * 0.7f),
                    style = stroke,
                )
            }

            DrawerIcon.Rules -> {
                val shield =
                    Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.86f, h * 0.22f)
                        lineTo(w * 0.86f, h * 0.52f)
                        cubicTo(w * 0.86f, h * 0.76f, w * 0.66f, h * 0.9f, w * 0.5f, h * 0.96f)
                        cubicTo(w * 0.34f, h * 0.9f, w * 0.14f, h * 0.76f, w * 0.14f, h * 0.52f)
                        lineTo(w * 0.14f, h * 0.22f)
                        close()
                    }
                drawPath(shield, color, style = stroke)
                drawLine(color, Offset(w * 0.34f, h * 0.5f), Offset(w * 0.46f, h * 0.62f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.46f, h * 0.62f), Offset(w * 0.68f, h * 0.38f), stroke.width, StrokeCap.Round)
            }

            DrawerIcon.Guide -> {
                drawRoundRect(
                    color,
                    topLeft = Offset(w * 0.14f, h * 0.1f),
                    size = Size(w * 0.72f, h * 0.8f),
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(w * 0.1f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.32f, h * 0.1f), Offset(w * 0.32f, h * 0.9f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.46f, h * 0.34f), Offset(w * 0.7f, h * 0.34f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.46f, h * 0.52f), Offset(w * 0.7f, h * 0.52f), stroke.width, StrokeCap.Round)
            }

            DrawerIcon.Logout -> {
                val door =
                    Path().apply {
                        moveTo(w * 0.45f, h * 0.12f)
                        lineTo(w * 0.15f, h * 0.12f)
                        lineTo(w * 0.15f, h * 0.88f)
                        lineTo(w * 0.45f, h * 0.88f)
                    }
                drawPath(door, color, style = stroke)
                drawLine(color, Offset(w * 0.4f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.88f, h * 0.5f), Offset(w * 0.7f, h * 0.32f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.88f, h * 0.5f), Offset(w * 0.7f, h * 0.68f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

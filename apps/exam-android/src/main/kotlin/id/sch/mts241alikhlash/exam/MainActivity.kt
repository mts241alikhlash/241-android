package id.sch.mts241alikhlash.exam

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.sch.mts241alikhlash.exam.data.auth.SupervisorResult
import id.sch.mts241alikhlash.exam.data.local.ExamDatabase
import id.sch.mts241alikhlash.exam.data.remote.ApiErrorCode
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.remote.userMessage
import id.sch.mts241alikhlash.exam.data.session.ExamRuntimePolicy
import id.sch.mts241alikhlash.exam.data.session.RestoreOutcome
import id.sch.mts241alikhlash.exam.data.session.endExamSession
import id.sch.mts241alikhlash.exam.data.session.restoreExamSession
import id.sch.mts241alikhlash.exam.data.session.startExamSession
import id.sch.mts241alikhlash.exam.kiosk.AudioAndDisplayGuard
import id.sch.mts241alikhlash.exam.kiosk.DndController
import id.sch.mts241alikhlash.exam.kiosk.InterferenceState
import id.sch.mts241alikhlash.exam.kiosk.InternalIntentGuard
import id.sch.mts241alikhlash.exam.kiosk.KioskPinRetryPolicy
import id.sch.mts241alikhlash.exam.kiosk.KioskPolicy
import id.sch.mts241alikhlash.exam.kiosk.SessionEventTracker
import id.sch.mts241alikhlash.exam.kiosk.ViolationPolicy
import id.sch.mts241alikhlash.exam.kiosk.ViolationStore
import id.sch.mts241alikhlash.exam.kiosk.blocksExamKey
import id.sch.mts241alikhlash.exam.proctor.ProctorController
import id.sch.mts241alikhlash.exam.proctor.ProctorFlags
import id.sch.mts241alikhlash.exam.proctor.ProctorSnapshotApi
import id.sch.mts241alikhlash.exam.ui.AppRootState
import id.sch.mts241alikhlash.exam.ui.AssessmentAppRoot
import id.sch.mts241alikhlash.exam.ui.ExamInputLockdown
import id.sch.mts241alikhlash.exam.ui.FocusShieldOverlay
import id.sch.mts241alikhlash.exam.ui.KioskWarningOverlay
import id.sch.mts241alikhlash.exam.ui.ReloginDialog
import id.sch.mts241alikhlash.exam.ui.SupervisorExitDialog
import id.sch.mts241alikhlash.exam.ui.components.batteryLevelNow
import id.sch.mts241alikhlash.exam.ui.components.isBatteryUnrestrictedNow
import id.sch.mts241alikhlash.exam.ui.components.isChargingNow
import id.sch.mts241alikhlash.exam.ui.components.isLockTaskActiveNow
import id.sch.mts241alikhlash.exam.ui.components.isOnlineNow
import id.sch.mts241alikhlash.exam.ui.screens.ExamListViewModel
import id.sch.mts241alikhlash.exam.ui.screens.ExamScreen
import id.sch.mts241alikhlash.exam.ui.screens.ExamViewModel
import id.sch.mts241alikhlash.exam.ui.screens.HomeScreen
import id.sch.mts241alikhlash.exam.ui.screens.LoginScreen
import id.sch.mts241alikhlash.exam.ui.screens.LoginViewModel
import id.sch.mts241alikhlash.exam.ui.screens.ProfileViewModel
import id.sch.mts241alikhlash.exam.ui.theme.BrandSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var violations: ViolationStore
    private lateinit var dnd: DndController
    private var audioGuard: AudioAndDisplayGuard? = null
    private var audioGuardJob: kotlinx.coroutines.Job? = null
    private var proctorController: ProctorController? = null
    private var tracker by mutableStateOf<SessionEventTracker?>(null)
    private var lastLocked: Boolean? = null
    private val guard = InternalIntentGuard.shared
    private val pinPolicy = KioskPinRetryPolicy()

    private var isExamActive = false
        set(value) {
            field = value
            (application as? ExamApplication)?.examActive = value
        }

    private val countedViolations = kotlinx.coroutines.flow.MutableStateFlow(0)
    private val activeInterference = kotlinx.coroutines.flow.MutableStateFlow(InterferenceState())
    private var showSupervisorExit by mutableStateOf(false)
    private var resumeCount by mutableIntStateOf(0)
    private var sessionRestored: Boolean? = null
    private var appState by mutableStateOf(AppRootState())
    private val hasWindowFocus = mutableStateOf(true)
    private var runningSessionId: String? = null
    private val reentrySignal = mutableStateOf<String?>(null)
    private val kioskGateOpen = mutableStateOf(false)

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pinRecheck = Runnable { verifyPinState() }
    private val pinRetryRecheck = Runnable { checkPinRetry() }

    private val pinWatch =
        object : Runnable {
            override fun run() {
                if (!isExamActive) return
                verifyPinState()
                mainHandler.postDelayed(this, 1000)
            }
        }

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = config.fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surface = BrandSurface.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(surface, surface),
            navigationBarStyle = SystemBarStyle.light(surface, surface),
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { window.setHideOverlayWindows(true) }
        }

        val app = application as ExamApplication
        violations = ViolationStore(this)
        dnd = DndController(this)
        if (!isExamActive) dnd.restorePreviousDnd()

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val covering = insets.isVisible(WindowInsetsCompat.Type.ime())
            tracker?.onCoveringWindowChanged(covering, System.currentTimeMillis())
            insets
        }

        setContent {
            id.sch.mts241alikhlash.exam.proctor
                .RequestCameraOnLaunch(guard)

            var restoreAttempt by remember { mutableStateOf(0) }

            LaunchedEffect(restoreAttempt) {
                appState = appState.copy(restoreError = null)
                when (
                    val outcome =
                        restoreExamSession(
                            sessionRepository = app.sessionRepository,
                            scope = lifecycleScope,
                            syncPending = app.repository::syncPending,
                            validate = { id ->
                                app.examActive = true
                                app.repository.pullServerAnswers(id)
                            },
                            syncClock = { id ->
                                app.examActive = true
                                app.repository.loadPaper(id, forceRefresh = true).getOrThrow()
                            },
                            isSessionFinished = { id ->
                                app.catalogRepository
                                    .listExams()
                                    .any { it.sessionId == id && it.status == "FINISHED" }
                            },
                            requiresTokenReentry = { id ->
                                val upload =
                                    runCatching {
                                        app.repository.uploadSessionEvents(id)
                                    }.getOrNull()
                                if (upload?.lockedByViolation == true) {
                                    runCatching {
                                        app.sessionRepository.markReentryPending(id)
                                    }
                                    true
                                } else if (upload?.requiresTokenReentry == true) {
                                    runCatching {
                                        app.sessionRepository.markReentryPending(id)
                                    }
                                    true
                                } else {
                                    runCatching {
                                        app.sessionRepository.reentryPending(id)
                                    }.getOrDefault(false)
                                }
                            },
                        ) { result ->
                            result.exceptionOrNull()?.let {
                                android.util.Log.w("MainActivity", "Startup answer sync failed", it)
                            }
                        }
                ) {
                    is RestoreOutcome.Restored -> {
                        isExamActive = true
                        sessionRestored = true
                        startKioskMode()
                        appState =
                            AppRootState(
                                restoreComplete = true,
                                user = runCatching { app.authRepository.currentStudent() }.getOrNull(),
                                sessionId = outcome.session.sessionId,
                                deadline = outcome.session.deadline,
                            )
                    }

                    is RestoreOutcome.TokenReentry -> {
                        isExamActive = true
                        sessionRestored = true
                        startKioskMode()
                        appState =
                            AppRootState(
                                restoreComplete = true,
                                user = runCatching { app.authRepository.currentStudent() }.getOrNull(),
                                reentrySessionId = outcome.session.sessionId,
                                deadline = outcome.session.deadline,
                            )
                    }

                    is RestoreOutcome.SessionLost -> {
                        app.examActive = false
                        val storedUser = runCatching { app.authRepository.currentStudent() }.getOrNull()
                        appState =
                            AppRootState(
                                restoreComplete = true,
                                user = storedUser,
                                notice = outcome.message,
                            )
                    }

                    is RestoreOutcome.AlreadyFinished -> {
                        app.examActive = false
                        violations.reset()
                        val storedUser = runCatching { app.authRepository.currentStudent() }.getOrNull()
                        appState =
                            AppRootState(
                                restoreComplete = true,
                                user = storedUser,
                                notice = outcome.message,
                            )
                    }

                    is RestoreOutcome.Failed -> {
                        appState =
                            AppRootState(
                                restoreComplete = true,
                                restoreError = outcome.message,
                            )
                    }

                    RestoreOutcome.NoSession -> {
                        val storedUser = runCatching { app.authRepository.currentStudent() }.getOrNull()
                        appState = AppRootState(restoreComplete = true, user = storedUser)
                    }
                }
            }

            LaunchedEffect(appState.sessionId) {
                val running = appState.sessionId
                runningSessionId = running
                if (running != null && appState.activeExam == null) {
                    val exam =
                        runCatching { app.catalogRepository.listExams() }
                            .getOrNull()
                            ?.firstOrNull { it.sessionId == running }
                    if (exam != null && appState.sessionId == running) {
                        appState = appState.copy(activeExam = exam)
                    }
                }
            }

            LaunchedEffect(reentrySignal.value) {
                val pending = reentrySignal.value ?: return@LaunchedEffect
                reentrySignal.value = null
                if (appState.sessionId == pending) {
                    appState =
                        appState.copy(
                            reentrySessionId = pending,
                            reentryLoading = false,
                            reentryError = null,
                        )
                }
            }

            val interference by activeInterference.collectAsState()
            val shieldInterference = interference.dominant
            val focusHeld by hasWindowFocus
            val gateOpen by kioskGateOpen

            AssessmentAppRoot(
                state = appState,
                onRetryRestore = {
                    appState = appState.copy(restoreComplete = false)
                    restoreAttempt++
                },
                onBackToList = {
                    appState =
                        appState.copy(
                            selectedExam = null,
                            readinessComplete = false,
                            startError = null,
                        )
                },
                onReadinessConfirmed = { appState = appState.copy(readinessComplete = true) },
                onSupervisorExit = { showSupervisorExit = true },
                onResumeWithToken = { accessCode ->
                    val sessionId = appState.reentrySessionId
                    if (sessionId != null && !appState.reentryLoading) {
                        appState = appState.copy(reentryLoading = true, reentryError = null)
                        lifecycleScope.launch {
                            val resumed =
                                runCatching {
                                    app.repository.resumeSession(sessionId, accessCode)
                                }
                            val failure = resumed.exceptionOrNull()
                            if (failure != null) {
                                val lost =
                                    (failure as? ApiException)?.let {
                                        it.statusCode == 401 ||
                                            it.statusCode == 404 ||
                                            it.code == ApiErrorCode.SESSION_LOST
                                    } ?: false
                                if (lost) {
                                    app.sessionRepository.finishIfCurrent(sessionId)
                                    app.examActive = false
                                    appState =
                                        appState.copy(
                                            reentryLoading = false,
                                            reentryError = null,
                                            reentrySessionId = null,
                                            selectedExam = null,
                                            readinessComplete = false,
                                            startError = failure.userMessage(),
                                        )
                                    return@launch
                                }
                                appState =
                                    appState.copy(
                                        reentryLoading = false,
                                        reentryError = failure.userMessage(),
                                    )
                                return@launch
                            }
                            val session = resumed.getOrThrow()
                            session.deadlineMillis?.let {
                                app.sessionRepository.retarget(sessionId, it)
                            }
                            val upload =
                                runCatching {
                                    app.repository.uploadSessionEvents(sessionId)
                                }.getOrNull()
                            if (upload?.lockedByViolation == true) {
                                appState =
                                    appState.copy(
                                        reentryLoading = false,
                                        reentryError = LOCKED_BY_VIOLATION_MESSAGE,
                                    )
                                return@launch
                            }
                            if (session.requiresTokenReentry || upload?.requiresTokenReentry == true) {
                                app.sessionRepository.markReentryPending(sessionId)
                                appState =
                                    appState.copy(
                                        reentryLoading = false,
                                        reentryError = null,
                                    )
                                return@launch
                            }
                            app.sessionRepository.clearReentryPending(sessionId)
                            appState =
                                appState.copy(
                                    reentryLoading = false,
                                    reentrySessionId = null,
                                    sessionId = sessionId,
                                    deadline = session.deadlineMillis ?: appState.deadline,
                                )
                        }
                    }
                },
                onStartExam = { accessCode ->
                    val exam = appState.selectedExam
                    if (exam != null && !appState.starting) {
                        appState = appState.copy(starting = true, startError = null)
                        lifecycleScope.launch {
                            val started =
                                runCatching {
                                    startExamSession(
                                        app.catalogRepository,
                                        app.sessionRepository,
                                        exam.id,
                                        accessCode,
                                        pullAnswers = { id ->
                                            app.examActive = true
                                            app.repository.pullServerAnswers(id)
                                        },
                                    )
                                }
                            val failure = started.exceptionOrNull()
                            if (failure != null) {
                                appState =
                                    appState.copy(
                                        starting = false,
                                        startError = failure.userMessage(),
                                    )
                                return@launch
                            }
                            isExamActive = true
                            sessionRestored = false
                            startKioskMode()
                            val deadline = runCatching { app.sessionRepository.deadline() }.getOrNull()
                            appState =
                                appState.copy(
                                    starting = false,
                                    selectedExam = null,
                                    readinessComplete = false,
                                    activeExam = exam,
                                    sessionId = started.getOrThrow().sessionId,
                                    deadline = deadline,
                                )
                        }
                    }
                },
                loginContent = {
                    val loginViewModel =
                        viewModel<LoginViewModel>(
                            factory =
                                viewModelFactory {
                                    initializer { LoginViewModel(app.authRepository) }
                                },
                        )
                    val loggedInUser by loginViewModel.loggedIn.collectAsState()
                    LaunchedEffect(loggedInUser) {
                        loggedInUser?.let {
                            appState = appState.copy(user = it)
                            loginViewModel.consumed()
                        }
                    }
                    LoginScreen(loginViewModel)
                },
                examListContent = {
                    val listViewModel =
                        viewModel<ExamListViewModel>(
                            key = "exam-list-${appState.user?.id}",
                            factory =
                                viewModelFactory {
                                    initializer { ExamListViewModel(app.catalogRepository) }
                                },
                        )
                    val profileViewModel =
                        viewModel<ProfileViewModel>(
                            key = "profile-${appState.user?.id}",
                            factory =
                                viewModelFactory {
                                    initializer {
                                        ProfileViewModel(app.profileRepository, appState.user?.name.orEmpty())
                                    }
                                },
                        )
                    HomeScreen(
                        userName = appState.user?.name.orEmpty(),
                        listViewModel = listViewModel,
                        profileViewModel = profileViewModel,
                        notice = appState.notice,
                        onSelect = {
                            appState =
                                appState.copy(
                                    selectedExam = it,
                                    readinessComplete = false,
                                    startError = null,
                                    notice = null,
                                )
                        },
                        onLogout = {
                            lifecycleScope.launch {
                                app.authRepository.logout()
                                appState = appState.copy(user = null, selectedExam = null)
                            }
                        },
                    )
                },
                examContent = { sessionId, deadline ->
                    LaunchedEffect(sessionId) {
                        violations.beginSession(sessionId)
                        countedViolations.value = violations.counted()
                        ExamDatabase.getInstance(applicationContext).sessionEventDao().prune(sessionId)
                        tracker =
                            SessionEventTracker(
                                focusThresholdMs = ExamRuntimePolicy.current.focusThresholdMs,
                                dao = ExamDatabase.getInstance(applicationContext).sessionEventDao(),
                                sessionId = sessionId,
                                scope = lifecycleScope,
                                guard = guard,
                                onViolationRecorded = {
                                    lifecycleScope.launch {
                                        val upload =
                                            runCatching {
                                                app.repository.uploadSessionEvents(sessionId)
                                            }.getOrNull()
                                        if (upload?.lockedByViolation == true) {
                                            app.sessionRepository.markReentryPending(sessionId)
                                        }
                                    }
                                },
                            ).also {
                                val now = System.currentTimeMillis()
                                it.onCallOrMicChanged(interference.callOrMic, now)
                                it.onSecondaryDisplayChanged(interference.screenShare, now)
                                it.onSuspensionChanged(interference.callOrMic)
                            }
                    }
                    LaunchedEffect(sessionId) {
                        while (true) {
                            delay(ExamRuntimePolicy.current.heartbeatIntervalMs)
                            val beat = app.repository.heartbeat(sessionId) ?: continue
                            if (beat.lockedByViolation) {
                                app.sessionRepository.markReentryPending(sessionId)
                            }
                        }
                    }
                    if (isExamActive && !gateOpen) {
                        KioskWarningOverlay(
                            onRetry = {
                                runCatching { startLockTask() }
                                mainHandler.removeCallbacks(pinRetryRecheck)
                                mainHandler.postDelayed(pinRetryRecheck, 400)
                            },
                            onSupervisorAccess = { showSupervisorExit = true },
                            onOpenPinSettings = {
                                guard.markLaunchingInternalDialog()
                                runCatching {
                                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                }
                            },
                        )
                        return@AssessmentAppRoot
                    }
                    val viewModel =
                        viewModel<ExamViewModel>(
                            key = "exam-$sessionId",
                            factory =
                                viewModelFactory {
                                    initializer {
                                        ExamViewModel(
                                            repository = app.repository,
                                            sessionId = sessionId,
                                            deadline = deadline,
                                            restored = sessionRestored,
                                            clock = app.serverClock,
                                            onSubmitConfirmed = { id -> app.sessionRepository.finishIfCurrent(id) },
                                            onDeadlineExtended = { extended ->
                                                app.sessionRepository.retarget(sessionId, extended)
                                            },
                                            isOnline = { applicationContext.isOnlineNow() },
                                            batteryLevel = { applicationContext.batteryLevelNow() },
                                            isCharging = { applicationContext.isChargingNow() },
                                            dndGranted = { dnd.isAccessGrantedOrNull() },
                                            lockTaskActive = { applicationContext.isLockTaskActiveNow() },
                                            batteryUnrestricted = { applicationContext.isBatteryUnrestrictedNow() },
                                        )
                                    }
                                },
                        )

                    val counted by countedViolations.collectAsState()
                    LaunchedEffect(counted) {
                        viewModel.recordViolations(reportedViolationCount(counted))
                    }
                    val serverViolations by viewModel.uiState.collectAsState()
                    LaunchedEffect(serverViolations.serverViolations) {
                        violations.restore(serverViolations.serverViolations)
                        countedViolations.value = violations.counted()
                    }

                    val proctor =
                        remember {
                            if (!ProctorFlags.ENABLED) {
                                null
                            } else {
                                ProctorController.camera(
                                    intervalMs = ExamRuntimePolicy.current.snapshotIntervalMs,
                                    context = applicationContext,
                                    api = app.retrofit.create(ProctorSnapshotApi::class.java),
                                    isAnswerSyncBusy = { app.repository.isSyncingOrSubmitting },
                                )
                            }
                        }
                    DisposableEffect(proctor) {
                        proctorController = proctor
                        onDispose { proctorController = null }
                    }
                    LaunchedEffect(sessionId, viewModel) {
                        activeInterference.collect { current ->
                            viewModel.onCallOrMicChanged(current.callOrMic, System.currentTimeMillis())
                        }
                    }

                    Box {
                        ExamInputLockdown {
                            ExamScreen(
                                viewModel = viewModel,
                                sessionId = sessionId,
                                proctor = proctor,
                                exam = appState.activeExam,
                                baseUrl = BuildConfig.BASE_URL,
                                imageLoader = app.imageLoader,
                                onSupervisorExit = { showSupervisorExit = true },
                                onSessionLost = { message ->
                                    lifecycleScope.launch {
                                        runCatching { app.sessionRepository.finishIfCurrent(sessionId) }
                                        releaseExamAndShowList(message)
                                    }
                                },
                                onFinish = {
                                    lifecycleScope.launch {
                                        val ended =
                                            runCatching {
                                                endExamSession(
                                                    sessionRepository = app.sessionRepository,
                                                    sessionId = sessionId,
                                                    clearQuestions = app.repository::clearQuestions,
                                                )
                                            }.getOrElse { error ->
                                                android.util.Log.e("MainActivity", "Teardown failed", error)
                                                false
                                            }

                                        val unsent = viewModel.uiState.value.unsentAnswers
                                        releaseExamAndShowList(
                                            when {
                                                viewModel.uiState.value.violationExit -> ViolationPolicy.warning(ViolationPolicy.LIMIT + 1)
                                                unsent > 0 -> "Ujian dikumpulkan, tetapi $unsent jawaban tidak sempat terkirim"
                                                ended -> "Ujian berhasil dikumpulkan"
                                                else -> "Ujian selesai, tetapi sesi gagal dibersihkan"
                                            },
                                        )
                                    }
                                },
                            )
                        }
                        if (isExamActive && (!focusHeld || shieldInterference != null)) {
                            FocusShieldOverlay(interference = shieldInterference)
                        }
                    }
                },
            )

            val tokensRejected by app.authRepository.rejected.collectAsState()
            var reloginDismissed by remember { mutableStateOf(false) }
            LaunchedEffect(tokensRejected, resumeCount) {
                if (tokensRejected) reloginDismissed = false
            }
            val activeSessionId = appState.sessionId
            if (tokensRejected && activeSessionId != null && !reloginDismissed) {
                ReloginDialog(
                    onSubmit = { identifier, password ->
                        val failure =
                            runCatching { app.authRepository.relogin(identifier, password) }
                                .exceptionOrNull()
                        if (failure == null) {
                            lifecycleScope.launch {
                                runCatching { app.repository.syncPending(activeSessionId) }
                            }
                        }
                        failure?.userMessage()
                    },
                    onDismiss = { reloginDismissed = true },
                )
            }

            if (showSupervisorExit) {
                SupervisorExitDialog(
                    onVerify = { identifier, password ->
                        app.supervisorVerifier.verify(identifier, password).also {
                            if (it != SupervisorResult.Approved) violations.record("supervisor_exit_denied")
                        }
                    },
                    onDismiss = { showSupervisorExit = false },
                    onConfirm = {
                        showSupervisorExit = false
                        emergencyExit()
                    },
                )
            }
        }
    }

    override fun onProvideAssistContent(outContent: android.app.assist.AssistContent?) = Unit

    override fun onProvideAssistData(data: Bundle?) = Unit

    private fun checkTokenReentry() {
        if (!isExamActive) return
        val running = runningSessionId ?: return
        lifecycleScope.launch {
            val pending =
                runCatching {
                    (application as ExamApplication).sessionRepository.reentryPending(running)
                }.getOrDefault(false)
            if (pending) reentrySignal.value = running
        }
    }

    private fun emergencyExit() {
        releaseExamAndShowList()
        violations.record("supervisor_exit")
        finishAndRemoveTask()
    }

    private fun warnAboutViolations() {
        countedViolations.value = violations.counted()
        val total = reportedViolationCount(countedViolations.value)
        Toast.makeText(this, ViolationPolicy.warning(total), Toast.LENGTH_LONG).show()
    }

    internal fun setExamActiveForTest(active: Boolean) {
        isExamActive = active
        applyExamWindowFlags()
    }

    internal fun setRootStateForTest(state: AppRootState) {
        appState = state
    }

    internal fun releaseExamForTest(notice: String? = null) = releaseExamAndShowList(notice)

    internal fun rootStateForTest(): AppRootState = appState

    private fun applyExamWindowFlags() {
        if (isExamActive) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            hideSystemBars()
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            showSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showSystemBars() {
        WindowCompat
            .getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun startKioskMode() {
        applyExamWindowFlags()
        kioskGateOpen.value = pinPolicy.gateOpen
        mainHandler.removeCallbacks(pinWatch)
        mainHandler.post(pinWatch)
        runCatching { startLockTask() }
        dnd.enableExamDnd()
        if (audioGuard == null) {
            val started = AudioAndDisplayGuard(this)
            audioGuard = started
            started.start()
            audioGuardJob =
                lifecycleScope.launch {
                    started.state.collect { current ->
                        activeInterference.value = current
                        val now = System.currentTimeMillis()
                        tracker?.onCallOrMicChanged(current.callOrMic, now)
                        tracker?.onSecondaryDisplayChanged(current.screenShare, now)
                        onCallOrMic(current.callOrMic)
                    }
                }
        }
    }

    private fun onCallOrMic(paused: Boolean) {
        if (!isExamActive) return
        tracker?.onSuspensionChanged(paused)
        if (paused) proctorController?.pause() else proctorController?.resume()
    }

    private fun releaseExamAndShowList(notice: String? = null) {
        val sessionId = appState.sessionId
        isExamActive = false
        reentrySignal.value = null
        stopKioskMode()
        violations.reset()
        appState = appState.afterRelease(notice)
        if (sessionId != null) {
            lifecycleScope.launch {
                runCatching { (application as ExamApplication).sessionRepository.clearReentryPending(sessionId) }
            }
        }
    }

    private fun stopKioskMode() {
        isExamActive = false
        kioskGateOpen.value = false
        pinPolicy.reset()
        lastLocked = null
        mainHandler.removeCallbacks(pinRecheck)
        mainHandler.removeCallbacks(pinWatch)
        mainHandler.removeCallbacks(pinRetryRecheck)
        applyExamWindowFlags()
        runCatching { stopLockTask() }
        dnd.restorePreviousDnd()
        audioGuardJob?.cancel()
        audioGuardJob = null
        audioGuard?.stop()
        audioGuard = null
        proctorController = null
        activeInterference.value = InterferenceState()
        tracker?.close()
        tracker = null
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (isExamActive && keyCode == KeyEvent.KEYCODE_BACK) {
            Toast
                .makeText(
                    this,
                    "Tombol kembali dinonaktifkan selama ujian berlangsung.",
                    Toast.LENGTH_SHORT,
                ).show()
            return true
        }
        if (blocksExamKey(isExamActive, keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (blocksExamKey(isExamActive, keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
        checkTokenReentry()
        guard.consumeOnResume()
        if (isExamActive) {
            applyExamWindowFlags()
            runCatching { startLockTask() }
            verifyPinState()
            mainHandler.removeCallbacks(pinRecheck)
            mainHandler.postDelayed(pinRecheck, 700)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isExamActive) return
        val interactive = getSystemService(android.os.PowerManager::class.java)?.isInteractive ?: true
        tracker?.onActivityStopped(interactive, System.currentTimeMillis())
        if (tracker?.requiresTokenReentry?.value == true) {
            val running = runningSessionId ?: return
            lifecycleScope.launch {
                (application as ExamApplication).sessionRepository.markReentryPending(running)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        this.hasWindowFocus.value = hasFocus
        tracker?.onWindowFocusChanged(hasFocus, System.currentTimeMillis())
        if (hasFocus && isExamActive) verifyPinState()
    }

    override fun onDestroy() {
        super.onDestroy()
        tracker?.close()
        tracker = null
        ExamRuntimePolicy.reset()
        audioGuardJob?.cancel()
        audioGuardJob = null
        audioGuard?.stop()
        audioGuard = null
        if (!isChangingConfigurations) dnd.restorePreviousDnd()
        mainHandler.removeCallbacks(pinWatch)
        mainHandler.removeCallbacks(pinRecheck)
        mainHandler.removeCallbacks(pinRetryRecheck)
    }

    private fun checkPinRetry() {
        val locked = KioskPolicy.isLocked(this)
        pinPolicy.onRetryAttempt(locked)
        recordPinLoss(locked)
        kioskGateOpen.value = pinPolicy.gateOpen
    }

    private fun verifyPinState() {
        val locked = KioskPolicy.isLocked(this)
        pinPolicy.onStateRefresh(locked)
        recordPinLoss(locked)
        kioskGateOpen.value = pinPolicy.gateOpen
    }

    internal fun reportedViolationCount(storeCount: Int): Int = storeCount

    private fun recordPinLoss(locked: Boolean) {
        val wasLocked = lastLocked
        lastLocked = locked
        if (!locked && wasLocked == true && isExamActive) {
            violations.record("pin_lost")
            warnAboutViolations()
        }
    }

    companion object {
        const val MIN_FONT_SCALE = 0.90f
        const val MAX_FONT_SCALE = 1.15f
        const val LOCKED_BY_VIOLATION_MESSAGE =
            "Sesi terkunci karena pelanggaran. Hubungi Teknisi Ruangan."
    }
}

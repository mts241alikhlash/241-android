package id.sch.mts241alikhlash.exam

import android.app.Application
import id.sch.mts241alikhlash.exam.api.exam.apis.StudentExamsApi
import id.sch.mts241alikhlash.exam.api.identity.apis.NativeAuthenticationApi
import id.sch.mts241alikhlash.exam.api.student.apis.StudentsApi
import id.sch.mts241alikhlash.exam.data.auth.AuthInterceptor
import id.sch.mts241alikhlash.exam.data.auth.AuthRepository
import id.sch.mts241alikhlash.exam.data.auth.KeystoreTokenStore
import id.sch.mts241alikhlash.exam.data.auth.SupervisorVerifier
import id.sch.mts241alikhlash.exam.data.auth.TokenAuthenticator
import id.sch.mts241alikhlash.exam.data.auth.authLoggingInterceptor
import id.sch.mts241alikhlash.exam.data.local.ExamDatabase
import id.sch.mts241alikhlash.exam.data.remote.DeviceIdInterceptor
import id.sch.mts241alikhlash.exam.data.repository.ExamCatalogRepository
import id.sch.mts241alikhlash.exam.data.repository.ExamRepository
import id.sch.mts241alikhlash.exam.data.session.DataStoreSessionStore
import id.sch.mts241alikhlash.exam.data.session.ServerClock
import id.sch.mts241alikhlash.exam.data.session.SessionRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ExamApplication : Application() {
    lateinit var repository: ExamRepository
        private set

    lateinit var sessionRepository: SessionRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var supervisorVerifier: SupervisorVerifier
        private set

    val serverClock: ServerClock = ServerClock()

    lateinit var catalogRepository: ExamCatalogRepository
    lateinit var profileRepository: id.sch.mts241alikhlash.exam.data.repository.StudentProfileRepository
        private set

    lateinit var imageLoader: coil3.ImageLoader
        private set

    lateinit var examApi: StudentExamsApi
        private set

    @Volatile
    var examActive: Boolean = false

    override fun onCreate() {
        super.onCreate()

        sessionRepository = SessionRepository(DataStoreSessionStore(this))

        val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        val logging =
            HttpLoggingInterceptor().apply {
                redactHeader("Authorization")
                level =
                    if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
            }

        val authClient =
            OkHttpClient
                .Builder()
                .addInterceptor(authLoggingInterceptor(BuildConfig.DEBUG))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

        val authApi =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(authClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(NativeAuthenticationApi::class.java)

        supervisorVerifier = SupervisorVerifier(authApi) { deviceId() }

        authRepository =
            AuthRepository(
                api = authApi,
                store = KeystoreTokenStore(this),
                deviceId = { deviceId() },
            )

        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(DeviceIdInterceptor { deviceId() })
                .addInterceptor(AuthInterceptor(authRepository))
                .authenticator(TokenAuthenticator(authRepository) { examActive })
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

        imageLoader =
            coil3.ImageLoader
                .Builder(this)
                .components {
                    add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { client }))
                }.build()

        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

        val api = retrofit.create(StudentExamsApi::class.java)
        examApi = api
        catalogRepository =
            ExamCatalogRepository(
                api = api,
                deviceId = { deviceId() },
                clock = serverClock,
            )
        profileRepository =
            id.sch.mts241alikhlash.exam.data.repository.StudentProfileRepository(
                retrofit.create(StudentsApi::class.java),
            )
        val database = ExamDatabase.getInstance(this)

        repository =
            ExamRepository(
                api,
                database.answerDao(),
                database.questionDao(),
                database.sessionEventDao(),
                deviceId = { deviceId() },
                onServerTime = serverClock::sync,
            )
    }

    fun deviceId(): String =
        android.provider.Settings.Secure
            .getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            .orEmpty()
}

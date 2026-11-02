plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.paparazzi)
}


fun String.asBaseUrl(): String = trim().let { if (it.endsWith("/")) it else "$it/" }
fun String.requireHttps(): String {
    require(startsWith("https://")) { "API_BASE_URL must use https:// for release builds, got '$this'" }
    return this
}
fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// Release signing credentials are read from -P Gradle properties or the matching
// environment variables. No keystore, alias, or password is ever stored in the repo.
// See docs/RELEASING.md.
val releaseSigningProperties = listOf(
    "RELEASE_STORE_FILE",
    "KEYSTORE_PASSWORD",
    "KEY_ALIAS",
    "KEY_PASSWORD",
)
val releaseSigningValues: Map<String, String?> = releaseSigningProperties.associateWith { name ->
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
}
val releaseStoreFilePath = releaseSigningValues["RELEASE_STORE_FILE"]
val releaseStoreFile = releaseStoreFilePath?.let { file(it) }
val hasReleaseStoreFile = releaseStoreFile?.exists() == true

// A typo'd RELEASE_STORE_FILE must fail here, not later with AGP's generic error.
val missingReleaseSigningInputs: List<String> = releaseSigningProperties.mapNotNull { name ->
    when {
        releaseSigningValues[name] == null -> name
        name == "RELEASE_STORE_FILE" && !hasReleaseStoreFile ->
            "$name (file not found: $releaseStoreFilePath)"
        else -> null
    }
}
val hasCompleteReleaseSigning = missingReleaseSigningInputs.isEmpty()

// The tasks that actually emit or sign a release artifact. Only these need
// credentials: `packageReleaseResources`, `bundleReleaseResources`,
// `signingConfigWriterRelease`, `bundleReleaseClassesToCompileJar` and every
// `*Test` task must run without them.
val releaseArtifactTaskNames = setOf(
    "packageRelease",
    "packageReleaseBundle",
    "packageReleaseUniversalApk",
    "signReleaseBundle",
)

// Fail fast when the *literal* packaging task was requested. This cannot be the only
// guard: Gradle accepts task-name abbreviations (`aR`, `assembleRel`, bare `a`), which
// never match here. The resolved task graph is validated below, after configuration.
val releasePackagingRequested = gradle.startParameter.taskNames.any { requested ->
    requested.substringAfterLast(':') in releaseArtifactTaskNames ||
        requested.substringAfterLast(':') in setOf(
            "assembleRelease", "bundleRelease", "installRelease", "publishReleaseBundle",
        )
}

val releaseSigningErrorMessage =
    "Release build needs -PRELEASE_STORE_FILE, -PKEYSTORE_PASSWORD, -PKEY_ALIAS, " +
        "-PKEY_PASSWORD (or the same names as environment variables). " +
        "Missing: ${missingReleaseSigningInputs.joinToString(", ")}. " +
        "See docs/RELEASING.md."

// Authoritative guard: inspect the *resolved* graph so abbreviations and any future
// alias that reaches a packaging task cannot ship an unsigned APK. Keyed on the tasks
// that produce the artifact, so non-packaging release tasks stay credential-free.
gradle.taskGraph.whenReady {
    val packagesRelease = allTasks.any { task ->
        task.project == project && task.name in releaseArtifactTaskNames
    }
    if (packagesRelease && !hasCompleteReleaseSigning) {
        throw GradleException(releaseSigningErrorMessage)
    }
}

android {
    namespace = "id.sch.mts241alikhlash.cbt"
    compileSdk = 35

    lint {
        warningsAsErrors = true
        disable += setOf(
            "LockedOrientationActivity",
            "NonResizeableActivity",
            "DiscouragedApi",
            "HardwareIds",
            "OldTargetApi",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "IconLauncherShape",
        )
        explainIssues = true
    }

    defaultConfig {
        applicationId = "id.sch.mts241alikhlash.cbt"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "BASE_URL",
            "http://10.0.2.2:4100/".asBuildConfigString(),
        )

    }

    flavorDimensions += "proctoring"
    productFlavors {
        create("standard") {
            dimension = "proctoring"
            buildConfigField("boolean", "PROCTORING_ENABLED", "false")
        }
        create("proctor") {
            dimension = "proctoring"
            buildConfigField("boolean", "PROCTORING_ENABLED", "true")
        }
    }

    signingConfigs {
        if (hasReleaseStoreFile) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningValues["KEYSTORE_PASSWORD"]
                keyAlias = releaseSigningValues["KEY_ALIAS"]
                keyPassword = releaseSigningValues["KEY_PASSWORD"]
            }
        }
    }

    buildTypes {
        debug {
            val debugUrl = providers.gradleProperty("API_BASE_URL_DEBUG")
                .orElse(providers.gradleProperty("API_BASE_URL"))
                .orElse("http://10.0.2.2:4100/")
                .get()
                .asBaseUrl()
            buildConfigField("String", "BASE_URL", debugUrl.asBuildConfigString())
        }
        release {
            val releaseUrl = providers.gradleProperty("API_BASE_URL")
                .orElse("https://exam-api.invalid/")
                .get()
                .asBaseUrl()
                .requireHttps()
            buildConfigField("String", "BASE_URL", releaseUrl.asBuildConfigString())
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasCompleteReleaseSigning) {
                signingConfig = signingConfigs.findByName("release")
            } else if (releasePackagingRequested) {
                throw GradleException(releaseSigningErrorMessage)
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Paparazzi and Robolectric cannot share a JVM: Paparazzi's layoutlib bridge
        // fails to initialise once Robolectric has run in the same process.
        unitTests.all { it.forkEvery = 1 }
    }

    sourceSets {
        // Robolectric unit tests read the exported Room schemas from the merged assets.
        // Only debug and androidTest carry them: the release APK must not ship test
        // fixtures, so `ExamDatabaseMigrationTest` skips itself when they are absent
        // (i.e. under `testReleaseUnitTest`).
        getByName("debug").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver)
}

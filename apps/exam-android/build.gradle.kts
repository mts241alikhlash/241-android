plugins {
    alias(libs.plugins.mts241.android.application.compose)
    alias(libs.plugins.mts241.room)
    alias(libs.plugins.mts241.lint)
    alias(libs.plugins.mts241.openapi.client)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
}

fun String.asBaseUrl(): String = trim().let { if (it.endsWith("/")) it else "$it/" }

fun String.requireHttps(): String {
    require(startsWith("https://")) { "API_BASE_URL must use https:// for release builds, got '$this'" }
    return this
}

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val releaseSigningProperties =
    listOf(
        "RELEASE_STORE_FILE",
        "KEYSTORE_PASSWORD",
        "KEY_ALIAS",
        "KEY_PASSWORD",
    )
val releaseSigningValues: Map<String, String?> =
    releaseSigningProperties.associateWith { name ->
        providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
            ?: providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
    }
val releaseStoreFilePath = releaseSigningValues["RELEASE_STORE_FILE"]
val releaseStoreFile = releaseStoreFilePath?.let { file(it) }
val hasReleaseStoreFile = releaseStoreFile?.exists() == true

val missingReleaseSigningInputs: List<String> =
    releaseSigningProperties.mapNotNull { name ->
        when {
            releaseSigningValues[name] == null -> name
            name == "RELEASE_STORE_FILE" && !hasReleaseStoreFile ->
                "$name (file not found: $releaseStoreFilePath)"
            else -> null
        }
    }
val hasCompleteReleaseSigning = missingReleaseSigningInputs.isEmpty()

val releaseArtifactTaskPattern =
    Regex("""^(package\w*Release(Bundle|UniversalApk)?|sign\w*ReleaseBundle)$""")

val releasePackagingRequested =
    gradle.startParameter.taskNames.any { requested ->
        requested.substringAfterLast(':').let { name ->
            releaseArtifactTaskPattern.matches(name) ||
                Regex("""^(assemble|bundle|install|publish)\w*Release(Bundle)?$""").matches(name)
        }
    }

val releaseSigningErrorMessage =
    "Release build needs -PRELEASE_STORE_FILE, -PKEYSTORE_PASSWORD, -PKEY_ALIAS, " +
        "-PKEY_PASSWORD (or the same names as environment variables). " +
        "Missing: ${missingReleaseSigningInputs.joinToString(", ")}. " +
        "See docs/guides/release-apk.md."

gradle.taskGraph.whenReady {
    val packagesRelease =
        allTasks.any { task ->
            task.project == project && releaseArtifactTaskPattern.matches(task.name)
        }
    if (packagesRelease && !hasCompleteReleaseSigning) {
        throw GradleException(releaseSigningErrorMessage)
    }
}

android {
    namespace = "id.sch.mts241alikhlash.exam"

    defaultConfig {
        applicationId = "id.sch.mts241alikhlash.exam"
        versionCode = 9
        versionName = "0.1.8"

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
            applicationIdSuffix = ".debug"
            val debugUrl =
                providers
                    .gradleProperty("API_BASE_URL_DEBUG")
                    .orElse(providers.gradleProperty("API_BASE_URL"))
                    .orElse("http://10.0.2.2:4100/")
                    .get()
                    .asBaseUrl()
            buildConfigField("String", "BASE_URL", debugUrl.asBuildConfigString())
        }
        release {
            val releaseUrl =
                providers
                    .gradleProperty("API_BASE_URL")
                    .filter { it.isNotBlank() }
                    .orElse("https://exam.mts241alikhlash.sch.id/")
                    .get()
                    .asBaseUrl()
                    .requireHttps()
            buildConfigField("String", "BASE_URL", releaseUrl.asBuildConfigString())
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasCompleteReleaseSigning) {
                signingConfig = signingConfigs.findByName("release")
            } else if (releasePackagingRequested) {
                throw GradleException(releaseSigningErrorMessage)
            }
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
        unitTests.all { it.forkEvery = 1 }
    }

    sourceSets {
        getByName("debug").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

tasks.named("check") { dependsOn("lintStandardDebug", "lintProctorDebug") }

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

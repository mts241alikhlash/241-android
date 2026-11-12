plugins {
    `kotlin-dsl`
}

group = "id.sch.mts241alikhlash.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.openapi.generator.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mts241.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "mts241.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "mts241.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "mts241.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "mts241.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("room") {
            id = "mts241.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("spotless") {
            id = "mts241.spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
        register("lint") {
            id = "mts241.lint"
            implementationClass = "LintConventionPlugin"
        }
        register("openApiClient") {
            id = "mts241.openapi.client"
            implementationClass = "OpenApiClientConventionPlugin"
        }
    }
}

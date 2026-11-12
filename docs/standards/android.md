# Android standards

## Modules

- `apps/<app>` is an app module: entry point, navigation root, flavors, signing, and code used by that app only. Why: a feature one app uses gains nothing from being shared and costs a module boundary.
- `core/<name>` is created only when a second app needs the code; extract it from the app then, not before. Why: guessing shared code early produces the wrong API. Check: review.
- Modules are named `:apps:<app>` and `:core:<name>`. The Kotlin package of an app is `id.sch.mts241alikhlash.<app>`. A `core` module never depends on an app module. Check: Gradle fails on a dependency cycle.

## Build configuration

- Shared values (`compileSdk`, `minSdk`, `targetSdk`, Java and Kotlin target, desugaring, lint options) are written only in `build-logic/convention`. Why: one place to upgrade. Check: review.
- An app build file applies convention plugins and writes only app-specific things: `applicationId`, version, flavors, `buildConfigField`, signing, dependencies.
- Plugin ids: `mts241.android.application`, `.application.compose`, `.library`, `.library.compose`, `mts241.jvm.library`, `mts241.room`, `mts241.lint`, `mts241.spotless`.
- Every dependency version is in `gradle/libs.versions.toml`. Why: one catalog, one upgrade path.

## Layering inside an app

- `data/` (remote, local, repository, session, auth) never imports from `ui/`. `ui/` talks to `data/` through repositories and ViewModels. Why: screens stay replaceable and data code stays testable without Compose. Check: review.

## Format

- Spotless with ktlint formats `**/*.kt` and `**/*.kts`; `./gradlew spotlessApply` fixes, `spotlessCheck` enforces in CI.

## Adding an app

1. `include(":apps:<app>")` in `settings.gradle.kts`.
2. `apps/<app>/build.gradle.kts` applies `mts241.android.application.compose` (add `mts241.room`,
   `mts241.lint` when it needs them) and writes only app-specific things: `applicationId`,
   `versionCode`, `versionName`, the release `API_BASE_URL` default, signing, dependencies.
3. Nothing else. CI covers every module through unqualified task names, and the release workflow reads
   the app and version from the tag `<app>@<version>` and builds `assembleRelease`.

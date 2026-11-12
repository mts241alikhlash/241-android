# 241 Android

Android apps of Madrasah Tsanawiyah 241 Al-Ikhlash, in one Gradle repository.

## Layout

| Path | What |
| --- | --- |
| `apps/istiqra` | Istiqra, the student exam app (kiosk mode) |
| `build-logic/` | Convention plugins (`mts241.*`); shared build config lives only here |
| `gradle/libs.versions.toml` | The single version catalog |
| `core/` | Shared modules, created when a second app needs them |
| `docs/` | Standards and guides |

## Quickstart

```bash
./gradlew --no-daemon spotlessApply
./gradlew --no-daemon spotlessCheck assembleDebug check
```

The second command is what CI runs; unqualified task names cover every module, so adding an app to
`apps/` needs no change here or in `.github/workflows/validate.yml`.

Requires JDK 17 or newer and the Android SDK (`ANDROID_HOME`). Releasing: `docs/guides/release-apk.md`.
Module rules: `docs/standards/android.md`.

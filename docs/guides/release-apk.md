# Release a signed APK

Release builds are signed with a keystore stored **outside** this repository. If the four inputs below are missing, the build fails at once with a message naming them, instead of producing an unsigned APK.

## Inputs

| Gradle property / env variable | Meaning |
| --- | --- |
| `RELEASE_STORE_FILE` | Absolute path to the `.jks` file |
| `KEYSTORE_PASSWORD` | Store password |
| `KEY_ALIAS` | Key alias (`istiqra`; one keystore per app) |
| `KEY_PASSWORD` | Key password |

## Create the keystore (once, by the owner, offline)

```bash
keytool -genkeypair -v \
  -keystore ~/keys/istiqra-release.jks \
  -alias istiqra \
  -keyalg RSA -keysize 2048 -validity 10000
```

One keystore per app: name it `<app>-release.jks` and use the matching alias, so uninstalling one app
never invalidates the other's signature.

Back it up offline before the first APK is distributed. Losing it means no installed device can ever be updated. Never commit it.

## Build locally

```bash
export RELEASE_STORE_FILE="$HOME/keys/istiqra-release.jks"
export KEYSTORE_PASSWORD='…' KEY_ALIAS='istiqra' KEY_PASSWORD='…'
./gradlew :apps:istiqra:assembleRelease
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --print-certs \
  apps/istiqra/build/outputs/apk/release/*.apk
```

Expected: the certificate matches the release key. `./gradlew :apps:istiqra:signingReport` exits 0 even without credentials; `Config: null` there means the APK would be unsigned.

## Release through CI

1. Raise `versionCode` (it must always increase) and set `versionName` in `apps/istiqra/build.gradle.kts`. While below `1.0.0`, a breaking change raises the minor version.
2. Merge, then push the tag `istiqra@<versionName>`. The workflow reads the app and version from the tag, fails if `versionName` differs, and publishes `istiqra-<versionName>.apk`.
3. The four repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` come from the same keystore.

## Installing over an old app

From 0.2.0 the application id is `id.sch.mts241alikhlash.istiqra` and the app is signed with the `istiqra` keystore, so Android treats it as a new app, not an update. On a device that still has the old exam app (`id.sch.mts241alikhlash.exam`), uninstall it first, then install Istiqra and redo the kiosk setup (`apps/istiqra/docs/KIOSK.md`). Tags before 0.2.0 are `exam-android@<version>`.

## Room migration 2 to 3

Version 3 rebuilds `cached_questions` (it gains `points`) and deletes synced rows of `cached_answers`. Unsent answers (`isSynced = 0`) are kept in the old plain-string format and the server will most likely reject them. Do not roll this out to a tablet that still holds unsent answers.

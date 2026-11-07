# 241 Android

Multi-app Gradle repository for the school's Android apps. Layout and quickstart: `README.md`.

## Build and test

- The check suite is listed in `README.md`; run it before every PR. Format with `./gradlew spotlessApply`.
- Run Gradle with `--no-daemon` on the shared laptop: daemons keep gigabytes of RAM that other sessions need.

## Standards

- Module, plugin and layering rules: `docs/standards/android.md`. Read it before adding a module or changing a build file.
- App-specific rules: the `AGENTS.md` and `README.md` inside `apps/<app>/`.

## Rules

- Stage files by path, never `git add -A` or `git add .`: build output and local keystores sit next to tracked files.
- Never commit a keystore, password or `keystore.properties`; signing material stays outside the repo. See `docs/guides/release-apk.md`.
- Versions of AGP, Kotlin, SDKs and libraries change only in `gradle/libs.versions.toml` and the convention plugins, in their own PR, so a behavior bug is never mixed with an upgrade.

## PR and release

`CONTRIBUTING.md` for PRs; `docs/guides/release-apk.md` for releases.

Server and personal facts: `AGENTS.local.md` (gitignored, may be absent), read before any deploy or server work.

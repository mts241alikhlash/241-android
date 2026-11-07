# Contributing

- Branch from `main`; `main` is protected, so every change is a PR with green CI.
- One concern per PR. Dependency upgrades never share a PR with behavior changes, so a regression points at one cause.
- Run the check suite from `README.md` before pushing; CI runs the same commands (`.github/workflows/validate.yml`).
- Run `./gradlew spotlessApply` first: `spotlessCheck` fails the PR on any formatting difference.
- Commit messages are Conventional Commits (`feat(<app>): ...`, `fix(exam-android): ...`, `chore(...)`), so release notes can be generated.
- A PR is done when CI is green and, for UI or kiosk changes, the app was tried on a device or emulator.
- A release raises `versionCode` and `versionName` in the app's `build.gradle.kts` and is tagged `<app>@<versionName>`; see `docs/guides/release-apk.md`.

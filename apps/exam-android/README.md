# exam-android

## Purpose

The student exam app: sign in with the school account, list exams, take an exam offline-tolerantly (questions and answers cached in Room, synced later), in kiosk mode. The `proctor` flavor adds camera snapshot proctoring; `standard` has no camera permission.

## Depends on

- Pinned OpenAPI contracts in `contracts/` (`exam: 0.3.0`, `identity: 0.1.1`, `student: 0.2.0`), synced from `241-apps` via `scripts/sync-contracts.sh` and generated at build time by `mts241.openapi.client`.
- Auth/session runtime rules (`docs/API-CONTRACT.md`).
- No `core/*` modules yet.

## Rules specific to this package

- Flavors `standard` and `proctor` must both build and pass tests. Why: they ship different permissions.
- Release signing inputs are the four `RELEASE_STORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; see `docs/guides/release-apk.md` at the repo root.
- Room schema changes need a migration and a new file in `schemas/`. Why: students may hold unsent answers.

## Gotchas

- Without Device Owner, Android shows a confirmation dialog on lock task: `docs/KIOSK.md`.
- Paparazzi and Robolectric cannot share a JVM, so unit tests fork per class.
- Camera trial setup: `docs/TRIAL-CAMERA.md`.
- The application id is new; an old installed app must be uninstalled first.

## Open work

- Dependency upgrades (stage 2).
- Extract `core/*` when a second app appears.

# exam-android

Read `README.md` here first, then `docs/standards/android.md` at the repo root.

- Keep `data/` free of `ui/` imports.
- Any Room entity change: bump the database version, add a migration, commit the new schema JSON.
- Test both flavors (`testStandardDebugUnitTest`, `testProctorDebugUnitTest`) before a PR.

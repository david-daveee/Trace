# Optional accounts and synchronization

Guest mode is fully functional. Settings offers optional Google sign-in through Firebase. Authentication alone never grants permission to overwrite data: the first cloud read is followed by an explicit source choice.

## Data choice

Plans, workout history, steps, language and covers follow the selected source. Completed walks merge by stable ID (legacy entries receive deterministic content IDs); deleted walk IDs prevent resurrection. Steps are selected, never added together. Current and paused workouts now follow the selected source, including set completion, weights, notes, stable IDs and plan links. Hardware counters, permissions, sensor enablement and active GPS recordings stay device-local. Live GPS recordings are preserved when applying either source. Automatic cloud replacement asks for an explicit choice when local unfinished sessions exist. Session IDs and plan references are validated before applying.

The app keeps the latest 20 app-private before-sync safety copies, available from backup history. Exported/manual backups are unaffected. Applying a snapshot validates it first and rolls back in-process failures. Restoring a backup resets cloud consent, requiring another source choice. An AtomicFile undo journal records the original preferences before applying. If the process dies before completion, startup restores those preferences before Store opens and suspends sync until the user chooses again. Isolated on-device instrumentation verifies interrupted-restore recovery, including float, long and boolean sensor preferences.

## Transport and conflicts

Each user has a private Firestore namespace. Snapshots are gzip compressed, bounded to 50 MB and split into 256 KB chunks with a SHA-256 manifest. Chunks stage before a head transaction checks the expected remote revision and publishes. Concurrent changes trigger a new source choice. A UID and operation generation guard reject stale operations after sign-out, restore or account changes. Already submitted network operations may finish; no subsequent stages or local apply continue after cancellation.

Successful publication retains the current and previous cloud revision and attempts cleanup of the older one. Interrupted staging or cleanup failures may leave orphaned revisions; maintenance is still required before broad distribution. Cloud rollback is currently an administrative recovery mechanism, not an in-app history screen.

Local data and base fingerprints persist across offline periods. Workout edits schedule a debounced retry after five seconds. Foreground retries also run every minute; Android schedules background retries at roughly 15-minute intervals subject to OS restrictions. A pending conflict pauses automatic retries until the user chooses. Signing out keeps local data and stops sync. A new account always requires consent.

## Validation and remaining release checks

Unit tests cover merge determinism, duplicate elimination, tombstones, both source choices, no step summing, local/cloud conflict decisions, bounded compressed transfer and checksum validation. Read-only phone instrumentation passed with the actual saved library and configured OAuth client. No user data was uploaded by these tests.

Owner Google sign-in and cloud transfer have succeeded. The new session-bearing snapshot was uploaded successfully on the connected phone. Two-device simultaneous changes, authentication expiry, interrupted transport and disk-failure recovery need end-to-end verification. An in-app account deletion flow and orphan cleanup are not implemented. Do not describe this development build as a fully verified public account release.

## New-phone flow

Install the latest APK, sign into the same Google account, and choose Load from account. The chosen snapshot restores current and paused sessions as well as plans and history; the workout tab resumes the saved set state. Workouts must finish syncing on the old phone before uninstalling. Old builds without session sync should be upgraded on every device. Live GPS recording cannot move between phones. Two-device simultaneous-edit integration still requires a second real device; unit tests cover revision decisions and both snapshot choices.

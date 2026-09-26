# Trace development notes

Trace 0.15.1 is an offline Android workout journal. Its application ID is `com.podhod.app`.

## Build

- Java 17 and Android Views
- Gradle 8.11.1 and Android Gradle Plugin 8.9.2
- Compile and target SDK 35
- Minimum SDK 26 (Android 8.0)

Open the root folder in Android Studio, configure JDK 17 and Android SDK 35, and allow Gradle sync to finish. Select your device and run the app.

```powershell
./gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. GitHub Releases currently distributes a debug-signed build, not a Google Play release. Keep signing consistent when updating an existing installation.

The optional Windows setup scripts download local dependencies into `.tools`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File scripts/build.ps1
```

The build script uses temporary ASCII-only directory junctions to avoid Java/Gradle issues with non-ASCII project paths. These point to the same project, not a separate checkout.

## App structure

The five tabs are Plans, My plans, Workout, Progress, and Steps. Program cards combine the schedule, editable maxes or working weights, and session actions. Local photos and vector icons work offline.

`Engine` owns data operations, percentage calculations, set completion, and program switching. `Store` persists state synchronously in private SharedPreferences. Completed workouts retain snapshots of their actual sets and weights.

Each program has independent progress. Switching programs pauses and parks the current session, then starts or restores the selected program. Removing a program from My plans retains its history, working weights, and unfinished session.

Any set can be marked or unmarked independently. Undo follows completion order rather than array order. Revision tokens reject stale repeated actions. Rest stages and timers have been removed; completing a set moves directly to the next incomplete set.

## Weight calculations

Sheiko plans use squat, bench press, and deadlift maxes. Changing maxes updates future percentage-based weights and remaining eligible sets in the current session. Completed sets and manual overrides are preserved. Rounding follows each source group.

The Zlat plan uses added working weights for pull-ups and dips. Progression uses each exercise's final planned set, regardless of the order in which sets were marked complete. The user confirms actual reps before saving. Increases are 5 kg for 8+ reps, 2.5 kg for 7, 1.25 kg for 6, 0.5 kg for 5, and zero for fewer than 5.

The public 2018 Zlat routine is organized into 12 journal sessions for convenience; this is not an author-imposed cycle length or the paid Advanced Beginner program.

Sources: [original video](https://www.youtube.com/watch?v=AeB4znuGuSo) and [forum translation](https://forum.steelfactor.ru/index.php?page=3&showtopic=46569).

## Built-in Sheiko plans

### CMS / MS preparation cycle

The supplied `Sheyko_podgotovka_plan_kmc_mc.xls` was converted into `app/src/main/assets/sheiko.json`: 16 days, 217 groups, 565 sets, and 178 percentage formula groups. Extracted calculations were checked against the workbook's saved values. The original workbook was not modified.

### Twelve-week competition cycle

The supplied `sheyko.xlsx` contains two preparation blocks followed by a competition block: 24 preparation workouts and 11 competition-phase workouts, totaling 1,251 sets. Competition #36 appears as an event after the schedule because the source does not prescribe attempts for it.

Run the extraction script with a local workbook path:

```powershell
python scripts/extract_sheyko_cycle.py path/to/sheyko.xlsx
```

The script requires `openpyxl` for reading. It writes `app/src/main/assets/sheiko_competition.json` and a local audit under `outputs/`. All 832 calculated weights were checked against the workbook's cached values. Default maxes are 175 / 150 / 180 kg, with 0.5 kg rounding; users should replace these maxes with their own. Missing accessory weights remain unspecified.

One source discrepancy is retained in the audit: on the competition sheet, cell D110 prescribes three reps at 60% in the summary, while detailed cells E112:F112 specify two. The imported plan follows the detailed cells.

Catalog insertion is idempotent and preserves existing programs, history, and sessions.

## Import formats

JSON preserves the program's percentage model. CSV supports UTF-8 with semicolon or tab delimiters. XLSX uses the first sheet with five columns and no formulas:

```csv
day;exercise;sets;reps;kg
Day 1;Squat;3;5;60
Day 1;Bench press;3;5;40
Day 2;Abs;3;10;0
```

Rows sharing a day name are grouped into one day. Each row remains an exercise block, preserving repeated exercises later in the session. Blank weight means a manual choice; zero means no additional load. Decimal commas are supported.

The original supported Sheiko XLS layout can retain its max-multiplication and rounding formulas. Arbitrary Excel layouts are not automatically supported. Formula-bearing XLSX input is rejected rather than relying on potentially stale cached results; the built-in 12-week cycle uses its dedicated extraction script.

## System workout panel

`WorkoutPanelService` provides a MediaSession-backed foreground service for a user-started workout. It does not play audio or request audio focus. Controls include set completion, pause/resume, undo, and opening the workout. Android controls the final panel appearance and lock-screen behavior.

## Language and progress

Display-only localization supports English and Russian. Language settings are stored separately from workout data. Built-in names are translated for display; user-created names and stored identifiers are preserved.

Progress uses saved workouts and local completion dates. Calendar intensity reflects completed set counts. Unfinished sessions are excluded. Weekly streaks allow the current week to remain open when the previous week was active.

## Validation

The 36 unit tests cover source formula preservation, recalculation, completed and manually overridden weights, program switching, max validation, independent completion and undo, imports, progress aggregation, catalog insertion, and step counter baselines, day transitions, resets, restarts, and duplicate readings.

## Phone step tracking

The optional Steps tab uses `TYPE_STEP_COUNTER`, with runtime `ACTIVITY_RECOGNITION` permission on Android 10+. A user-enabled health foreground service keeps the listener registered and uses a separate low-importance notification. On Android 14+, the service declares `FOREGROUND_SERVICE_HEALTH`. There is no GPS tracking or Health Connect integration. See [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types).

The first reading establishes a baseline instead of importing the device's lifetime count. Persisted deltas are grouped by the local date of the sensor event. A batch spanning midnight is assigned to the event date because individual step timestamps are unavailable. Reboots and decreasing counters establish a fresh baseline; disabling and re-enabling excludes the disabled interval. Duplicate and older readings from the same boot are ignored. After reboot or force-stop, the user must open Trace again; there is no boot receiver. Gaps in sensor delivery or service execution are not guaranteed to be recoverable.

Step data and goals use separate private preferences and are not included in workout backups. Workout state is unchanged by step tracking.

Device instrumentation checks UI rendering, language switching, and system-panel actions. Tests that use temporary data restore original state. The latest competition-cycle verification checked both interface languages and exact data equality before and after installation, apart from the new catalog entry.

## Data and backups

There is no server, analytics, or internet permission in the app. Backups include programs, history, and unfinished workouts. Restoring replaces current data only after confirmation. Save a backup before uninstalling.

Private backups, local SDK configuration, build outputs, dependency caches, and signing keys are excluded from Git.

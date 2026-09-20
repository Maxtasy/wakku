# Wakku — project context

Android alarm clock app. The premise: alarms ring, and actually stopping one
requires physically shaking the phone `NUMBER_OF_SHAKES` times — tapping
**Stop** alone only silences the alarm and starts that challenge; not
finishing it (cancel, back, home, walking away) falls back to snoozing
instead. Personal project first, Play Store release planned eventually. Free,
ad-free, no accounts, everything local (Room + DataStore, no backend).

Full naming/stack/scope rationale lives in the original blueprint artifact
(not reproduced here) — this file is a working-state handoff, not the
original pitch.

## Status

M0–M8 complete and pushed to `main`. **Next up: M9 (dogfood) — in progress.**
First real overnight alarm worked. Follow-ups from day one (persistent
"next alarm" notification, notification Stop action, live clock on the ringing
screen) are done; see the decisions below.

- M0 Project setup ✓
- M1 Alarm CRUD ✓
- M2 Reliable scheduling (AlarmManager, boot receiver) ✓
- M3 Ringing screen (foreground service, full-screen activity, sound+vibration) ✓
- M4 Shake-to-stop ✓
- M5 Snooze ✓
- M6 Settings (global snooze/shakes/vibration/sound) ✓
- Per-alarm settings ✓ (done between M6 and M7, out of the original blueprint's
  numbering — nullable override columns on `Alarm`; null means "use the global
  default from `SettingsRepository`". See `AlarmEditScreen`'s "Custom settings
  for this alarm" toggle and `RingingService`'s `alarm.xxx ?: defaults.xxx`
  pattern.)
- M7 Polish & compatibility ✓ (app icon and empty state turned out to already
  be done from earlier work; added dark-mode window theme, an accessibility
  pass, and the battery-optimization exemption prompt)
- M8 Testing & stability ✓ (unit tests for `ShakeCounter`/`AlarmTiming`;
  Compose UI tests for `AlarmEditScreen`/`RingingActivity`'s ringing screen)

## Tech stack

- Kotlin 2.3.20, Jetpack Compose (BOM 2026.09.00), Material3, Navigation-Compose 2.10.1
- Room 2.8.5 (local SQLite, no backend/sync)
- DataStore Preferences 1.2.1 (global settings)
- AGP 9.4.0 / Gradle 9.6.0 / KSP 2.3.12 — these were bumped several times
  during development after Android Studio's own auto-upgrades left things
  mismatched (Kotlin and KSP drifted out of sync at one point, causing a
  cryptic `unexpected JVM signature V` build failure). If a fresh sync ever
  fails looking like a Kotlin/KSP version mismatch, check these three are
  still a coherent set — KSP's own `gradle.properties` on GitHub
  (`google/ksp`, tag matching the version in use) states which Kotlin
  version it was built against.
- minSdk 26, compileSdk 37 (`compileSdkMinor = 2`), targetSdk 37
- Package: `com.maxtasy.wakku`

## Package structure

- `alarms/` — alarm list + create/edit screens, ViewModels
- `data/` — Room entity/DAO/database (`Alarm`, `AlarmDao`, `WakkuDatabase`, `Converters`)
- `scheduling/` — `AlarmScheduler` (wraps `AlarmManager.setAlarmClock`), `AlarmReceiver` (fires alarms), `BootReceiver`, `NextAlarmNotifier` (persistent "Next alarm" notification)
- `ringing/` — `RingingService` (foreground service: sound/vibration/notification), `RingingActivity` (full-screen UI, shake-challenge state machine), `RingingController` (shared StateFlow so the Activity can close itself if the notification's own action ends things externally)
- `shake/` — `ShakeCounter` (pure, unit-testable peak-detection logic) + `ShakeDetector` (SensorEventListener wrapper around it)
- `settings/` — `AppSettings`, `SettingsRepository` (DataStore), `SettingsViewModel`, `SettingsScreen`, `SettingsComponents` (shared `SettingSlider`/`VibrationSwitchRow`/`SoundPickerRow`, reused by `AlarmEditScreen` for per-alarm overrides)
- `ui/theme/` — Material3 color scheme, deliberately filled out beyond the 5 default roles (a partially-filled scheme leaks Material3's baseline purple into unset roles like Switch thumbs / FAB containers — happened once already, fixed in M3). `tertiary`/`error`/`inverseSurface`/etc. roles are still left at Material3 defaults in both light and dark — fine for now since nothing renders an error state yet, but fill them in if that changes.

`AlarmTiming` (in `scheduling/`) holds `AlarmScheduler`'s next-trigger-time
math as a pure `nextTriggerMillis(alarm, now, zone)` function — mirrors the
`ShakeCounter`/`ShakeDetector` split so it's unit testable without Android
framework classes.

## Key non-obvious decisions

- **`AlarmManager.setAlarmClock()`**, not `setExactAndAllowWhileIdle()` —
  gives the status-bar alarm-clock icon for free and is Doze-exempt. It
  still needs `USE_EXACT_ALARM` in the manifest on current Android — an
  earlier assumption that `setAlarmClock` was fully exempt from the
  exact-alarm permission was wrong, confirmed by a real crash log during M2
  testing.
- **Notification "Stop" action** (added in M9): the ringing notification has
  "Snooze" and "Stop". An earlier version (M6) removed Stop because it
  silenced the alarm with one tap and bypassed the shake challenge. The
  current Stop is an *activity* PendingIntent
  (`RingingActivity.EXTRA_START_CHALLENGE`) that opens `RingingActivity`,
  which silences the alarm and enters the shake challenge immediately — same
  as tapping its own Stop button, so the challenge is never bypassed (walking
  away still snoozes). Needed because with the phone unlocked the alarm shows
  only as a heads-up notification, not the full-screen screen. Its request
  code differs from the full-screen intent's (`REQUEST_CODE_STOP_OFFSET`),
  since PendingIntents that differ only in extras would otherwise collapse
  into one. `RingingActivity` is `singleTask`, so `onNewIntent` also handles
  the flag.
- **Persistent "Next alarm" notification** (M9): `NextAlarmNotifier` posts a
  low-importance ongoing notification (own `next_alarm` channel) showing the
  soonest pending trigger time. It replaced the old one-shot "Snoozed"
  notification, which never updated and lingered after the alarm was stopped.
  `AlarmScheduler.scheduleAt`/`cancel` record/remove each alarm's trigger time
  in SharedPreferences and refresh the notification, so snoozes, disabling,
  deleting and one-time alarms firing all keep it correct. It also serves as
  the "alarm is set" indicator when the status-bar icon doesn't show (it did
  appear once an alarm was enabled after this change, on the POCO).
  Known gap: a snooze pending across a reboot is rescheduled by
  `BootReceiver` to the regular time, not the snooze time.
- **Snoozed alarms have a way into the challenge** (M9): while an alarm is
  snoozed, the "Next alarm" notification switches to "Alarm snoozed / Rings
  again at HH:mm" and gains a **Stop** action (only visible when the
  notification is expanded). It opens `RingingActivity` with
  `EXTRA_DISMISS_SNOOZE` (`RingingActivity.dismissSnoozeIntent`): nothing is
  ringing, so it skips `ACTION_STOP`, loads the alarm's shake count from the
  DB, and starts the challenge. Completing it sends
  `RingingService.ACTION_DISMISS_SNOOZE` (one-time alarm: disable + cancel;
  repeating: `schedule()` back to the normal next occurrence). Cancelling or
  leaving (Home/back) does nothing — the existing snooze stays untouched, it
  must not re-snooze and push the time out. `NextAlarmNotifier` stores each
  trigger as `"millis,snoozed(0/1)"` in prefs; `AlarmScheduler.scheduleAt` has
  a `snoozed` flag that only `RingingService.snooze` sets.
- **Ringing screen** shows the *current* time (updates each minute), the alarm
  label (falls back to "Alarm" when blank) and a small "Set for HH:mm" line
  with the alarm's own time.
- **Snooze re-enables the alarm** in the DB. `AlarmReceiver` disables
  one-time alarms the instant they first ring (so the "done when" state is
  correct if the user never interacts again); without re-enabling on
  snooze, the alarm list would show "off" while the alarm was actually still
  live and counting down to re-ring.
- **`RingingActivity`'s shake challenge**: tapping Stop silences immediately
  and enters the challenge; not finishing it (cancel, back, home,
  task-switch — enforced via `onStop()` + an `isChangingConfigurations`
  guard so rotation doesn't falsely trigger it) falls back to snoozing
  rather than letting silence alone dismiss the alarm.
- The status-bar alarm-clock icon not showing on some devices (confirmed on
  a MIUI/HyperOS phone at first) turned out to be a platform/OEM rendering
  choice, not an app bug — it's a single system-wide slot for whichever
  app's alarm is soonest, and the underlying `AlarmManager` "next alarm
  clock" registration was independently verified correct via `dumpsys
  alarm`.
- **Battery-optimization exemption** (M7): `MainActivity` checks
  `PowerManager.isIgnoringBatteryOptimizations()` and `AlarmListScreen` shows
  a dismissible banner with a "Fix it" button that launches
  `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` when not exempted.
  On this MIUI device that intent doesn't show the stock Android allow/deny
  dialog — MIUI redirects it to its own "Akkudetails" (battery details)
  screen for the app, where the user has to manually pick "Keine
  Beschränkungen" (No restrictions). Confirmed working end-to-end on-device:
  the banner correctly disappears once that's selected, so MIUI's custom UI
  is just a themed front-end over the same underlying AOSP doze-whitelist
  state, not a separate mechanism.
- **Accessibility** (M7): label+switch rows (vibrate switch, per-alarm
  "Custom settings" switch) use `Modifier.toggleable(role = Role.Switch)` on
  the row with `Switch(onCheckedChange = null)`, merged via
  `Modifier.semantics(mergeDescendants = true)`, so screen readers announce
  one actionable node instead of two. The day-of-week `FilterChip`s show a
  single narrow letter visually but carry the full locale-aware day name
  (`Locale.getDefault()`) as their `contentDescription`, plus a 48dp minimum
  touch target.
- **`ShakeCounter` had a latent bug**, caught by its own unit tests (M8):
  `lastShakeAtMillis` defaulted to `0L`, so a reading timestamped at or near
  zero would have its first-ever shake silently suppressed (`atMillis - 0 <
  minIntervalMillis`). Harmless with the real `System.currentTimeMillis()`
  clock (always huge), but a real trap if that clock source ever changed
  (e.g. to `SystemClock.elapsedRealtime()`, which starts at 0 on boot). Now
  uses a nullable `Long?` "no prior shake yet" sentinel instead.

## Automated tests (added M8)

- **Unit tests** (`app/src/test/`, plain JUnit4, no device/emulator needed):
  `ShakeCounterTest`, `AlarmTimingTest`. Run with:
  ```bash
  gradle testDebugUnitTest
  ```
- **Instrumented Compose UI tests** (`app/src/androidTest/`):
  `AlarmEditScreenTest`, `RingingScreenTest` (the latter needed
  `RingingActivity`'s private `RingingScreen` composable made `internal`).
  These render the composables directly with `createComposeRule()` — no
  Activity/ViewModel wiring needed, since both screens take all state via
  params/callbacks. The shake challenge's own sensor-driven counting isn't
  exercised here (no way to fire real accelerometer events from a test); that
  logic is covered by `ShakeCounterTest` instead.

  **Do not use `gradle connectedDebugAndroidTest` on this MIUI device** — its
  own install/uninstall-then-reinstall cycle reliably triggers MIUI's
  "Installieren über USB" confirmation popup (a ~10–12s countdown that
  defaults to **Deny**), and repeated denials make MIUI silently add a
  persistent block for the package — found under Developer options →
  "Installieren über USB" → the list/submenu entry, which then has to be
  manually removed there before the app can be installed via `adb` again at
  all (not just for tests). Instead, install both APKs once via plain `adb`
  (which doesn't seem to trigger the same repeated-popup problem) and drive
  the instrumentation runner directly:
  ```bash
  adb install app/build/outputs/apk/debug/app-debug.apk
  adb install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  adb shell am instrument -w com.maxtasy.wakku.test/androidx.test.runner.AndroidJUnitRunner
  ```
  Re-run `adb install` (no `-r` needed the first time; use `-r` after) only
  for whichever APK actually changed.
  - The test device's locale is German. Any assertion on locale-dependent
    text (day names, etc.) must derive the expected string the same way
    production code does (`Locale.getDefault()`) rather than hardcoding
    English — `AlarmEditScreenTest` hardcoded "Monday" once and failed
    on-device because the real contentDescription was "Montag".
  - Use `assertExists()`, not `assertIsDisplayed()`, for elements that can be
    scrolled off-screen (e.g. `AlarmEditScreen`'s bottom controls) — existence
    is usually what the test actually cares about, and visibility depends on
    viewport size.

## Testing workflow used so far

No physical-device CI. Testing has been manual, via a real Android phone
(POCO F3 / MIUI-HyperOS, Android 13) driven through `adb` from the assistant
side rather than through Android Studio's Run button. Notes for whoever
picks this up next:

- **No `gradlew` is checked in.** Android Studio does not auto-generate it
  just from `File > Sync` (contrary to an earlier assumption) — the
  version-catalog Gradle version is still whatever `gradle-wrapper.properties`
  points at. Build from the command line via the cached wrapper distribution
  directly, e.g. (adjust the hash directory to whatever's actually under
  `~/.gradle/wrapper/dists/gradle-9.6.0-bin/` — it's a per-install hash):
  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  "/c/Users/<user>/.gradle/wrapper/dists/gradle-9.6.0-bin/<hash>/gradle-9.6.0/bin/gradle.bat" assembleDebug
  ```
- `adb install -r` needed **"USB debugging (Security settings)"** enabled in
  Developer Options on this MIUI device — a second, separate toggle from
  plain USB debugging — before installs worked at all. Also needed "MIUI
  optimization" considered as a first guess (it wasn't present on this
  EEA/Global ROM; the security-settings toggle was the actual fix).
- `uiautomator dump` crashes on this MIUI build (an unrelated MIUI
  theme-resource bug, not ours). Read the UI via screenshot
  (`adb exec-out screencap -p`) and estimate tap coordinates instead of
  relying on the accessibility tree.
- Reading/writing the Room DB directly for test setup (inserting a test
  alarm a minute out, etc.) needs `MSYS_NO_PATHCONV=1` prefixed on
  Git-Bash `adb push` / `adb shell run-as ...` calls, or leading `/data/...`
  paths get mangled into Windows paths.
- Quick ring test without waiting: force-stop the app, pull `wakku.db` (plus
  `-wal`/`-shm`) via `adb exec-out "run-as com.maxtasy.wakku cat databases/..."`,
  set the alarm's hour/minute a couple of minutes out with Python's `sqlite3`
  (`pragma wal_checkpoint(truncate)`), push it via `/data/local/tmp` and
  `run-as ... cp`, delete `-wal`/`-shm`, relaunch. The phone must be
  *unlocked* to see the heads-up notification path (locked shows the full
  screen). `adb` lives in `~/AppData/Local/Android/Sdk/platform-tools`.
- After pushing a modified `.db` file into place, **force-stop the app
  first** (not just after) — editing the DB file while the app process is
  still alive can leave it running on stale in-memory state that later
  overwrites the edit. Always: force-stop → edit DB file (+ delete
  `-wal`/`-shm`) → relaunch → verify with a screenshot before trusting the
  UI reflects the new state.
- A stray `AlarmManager` trigger left over from manual DB edits (e.g. a
  deleted test alarm that still has a pending fire) is harmless —
  `AlarmReceiver` no-ops silently if the alarm id it looked up no longer
  exists in the DB.
- **The lock screen re-sleeps the display ~10s after `KEYCODE_WAKEUP`** if
  nothing else touches it (`dumpsys power` shows
  `mUserActivityTimeoutOverrideFromWindowManager=10000` while the keyguard is
  showing — independent of the user's actual configured screen-timeout
  setting). Multi-step `adb` sequences (wake, then a separate swipe, then a
  separate screenshot) routinely lost the race and captured a black/asleep
  frame. Chain wake + the actual interaction into a **single** `adb shell
  "cmd1; cmd2; ..."` call to keep total latency under that window; once
  actually past the keyguard the normal (much longer) screen-timeout setting
  applies and this stops being an issue.
- A first-time `adb install` of a **new** package (not a reinstall) can pop a
  MIUI "Installieren über USB" confirmation with a ~10–12s countdown that
  defaults to **Deny** — see the automated-tests section above for what
  happens if it's missed a few times in a row (MIUI starts silently blocking
  the package) and how to undo it.

## Remaining milestones (from the original blueprint)

- **M9 — Dogfood**: two weeks of real daily use, phone as primary alarm,
  keep a backup alarm until confidence is there. Worth specifically watching
  whether the battery-optimization exemption (M7) actually keeps `RingingService`
  alive overnight on this MIUI device, since that was the whole point of adding it.
- **M10 — Play Store release**: signing key, store listing, screenshots,
  privacy policy page, closed testing track before production.

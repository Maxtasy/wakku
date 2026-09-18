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

M0–M6 complete and pushed to `main`. **Immediate next step below, before M7.**

- M0 Project setup ✓
- M1 Alarm CRUD ✓
- M2 Reliable scheduling (AlarmManager, boot receiver) ✓
- M3 Ringing screen (foreground service, full-screen activity, sound+vibration) ✓
- M4 Shake-to-stop ✓
- M5 Snooze ✓
- M6 Settings (global snooze/shakes/vibration/sound) ✓

## Immediate next step: per-alarm settings

The user wants **per-alarm overrides** for snooze time / shake count /
vibration / sound instead of one global setting (M6 shipped the global
version; per-alarm was deferred out of that session to keep context
manageable — do this next, before M7, unless told otherwise). Rough plan:

1. Add nullable override columns to the `Alarm` Room entity
   (`snoozeMinutes: Int?`, `numberOfShakes: Int?`, `vibrationEnabled: Boolean?`,
   `soundUri: String?`) — null means "use the global default from Settings."
   Bump `@Database` version; a destructive migration is fine, there's no real
   user data to preserve yet.
2. Extract the `SettingSlider` composable and the sound-picker row out of
   `settings/SettingsScreen.kt` into shared composables so `AlarmEditScreen`
   can reuse them for per-alarm overrides instead of duplicating that UI.
3. `RingingService` / `RingingActivity` should prefer the alarm's own
   override, falling back to `SettingsRepository`'s value when null.
4. Keep the global Settings screen as "defaults for new alarms."

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
- `scheduling/` — `AlarmScheduler` (wraps `AlarmManager.setAlarmClock`), `AlarmReceiver` (fires alarms), `BootReceiver`
- `ringing/` — `RingingService` (foreground service: sound/vibration/notification), `RingingActivity` (full-screen UI, shake-challenge state machine), `RingingController` (shared StateFlow so the Activity can close itself if the notification's own action ends things externally)
- `shake/` — `ShakeCounter` (pure, unit-testable peak-detection logic) + `ShakeDetector` (SensorEventListener wrapper around it)
- `settings/` — `AppSettings`, `SettingsRepository` (DataStore), `SettingsViewModel`, `SettingsScreen`
- `ui/theme/` — Material3 color scheme, deliberately filled out beyond the 5 default roles (a partially-filled scheme leaks Material3's baseline purple into unset roles like Switch thumbs / FAB containers — happened once already, fixed in M3)

## Key non-obvious decisions

- **`AlarmManager.setAlarmClock()`**, not `setExactAndAllowWhileIdle()` —
  gives the status-bar alarm-clock icon for free and is Doze-exempt. It
  still needs `USE_EXACT_ALARM` in the manifest on current Android — an
  earlier assumption that `setAlarmClock` was fully exempt from the
  exact-alarm permission was wrong, confirmed by a real crash log during M2
  testing.
- **Notification's "Stop" quick-action was deliberately removed** (M6) — it
  let people silence the alarm with one tap from the notification shade,
  completely bypassing the shake challenge. Only "Snooze" remains as a
  notification quick-action; Stop only exists inside the full
  `RingingActivity` screen.
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

## Remaining milestones (from the original blueprint)

- **M7 — Polish & compatibility**: app icon (still a placeholder), empty
  states, dark mode pass (theme already supports it, hasn't been visually
  reviewed), accessibility pass, battery-optimization exemption prompt
  (important on MIUI/Xiaomi/Samsung-style OEMs that aggressively kill
  background apps — worth explicitly testing whether Wakku survives being
  killed without requesting that exemption first).
- **M8 — Testing & stability**: unit tests for `ShakeCounter` (already
  structured to be testable without constructing a real `SensorEvent`) and
  the snooze/reschedule math in `AlarmScheduler`; Compose UI tests for the
  alarm form and ringing screen.
- **M9 — Dogfood**: two weeks of real daily use, phone as primary alarm,
  keep a backup alarm until confidence is there.
- **M10 — Play Store release**: signing key, store listing, screenshots,
  privacy policy page, closed testing track before production.

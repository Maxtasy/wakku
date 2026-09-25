# Wakku

An Android alarm clock you have to shake off. Free, ad-free, no accounts,
and everything stays on the device.

Tapping **Stop** only silences the alarm. To actually turn it off you have to
shake the phone a set number of times. If you give up partway (cancel, back,
home, or just put the phone down), the alarm snoozes instead of turning off.

Current version: **1.1.0** (see [CHANGELOG.md](CHANGELOG.md)).

## Features

- **Alarms**: one-time or repeating on chosen weekdays, with an optional
  label. Saving an alarm turns it on.
- **Reliable ringing**: exact alarms via `AlarmManager.setAlarmClock()`
  (not affected by Doze), rescheduled after a reboot. A foreground service
  plays the sound and vibration, and a full-screen ringing screen shows
  over the lock screen.
- **Shake to stop**: peak detection on the accelerometer counts the shakes.
- **Snooze**: from the ringing screen or the notification. While an alarm is
  snoozed, the notification offers **Stop**, which starts the shake
  challenge to cancel the snooze early.
- **"Next alarm" notification**: always shows when the next alarm will ring,
  and whether it's snoozed.
- **Settings**: snooze length, shakes needed to stop, vibration and alarm
  sound. Each alarm can override them.
- **Battery-optimization prompt**: a banner helps you exempt Wakku on
  devices that aggressively kill background apps (MIUI, etc.).
- Dark theme, basic screen-reader support.

## Tech stack

Kotlin, Jetpack Compose (Material3), MVVM, Navigation-Compose, Room,
DataStore Preferences. No backend and no network access.

- **Package**: `com.maxtasy.wakku`
- **Min SDK**: 26 (Android 8.0), **target SDK**: 37

## Building

Open the folder in [Android Studio](https://developer.android.com/studio)
and run the `app` configuration. There is no `gradlew` checked in yet.
To build from the command line, run Gradle 9.6.0 with Android Studio's
bundled JDK:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
gradle assembleDebug
```

## Release builds

Wakku isn't on the Play Store. Release APKs are shared directly
(sideloaded). An update only installs over an existing copy if it's
signed with the same key, so the release keystore must never be lost.
Keep a backup outside this machine.

One-time setup: create a keystore outside the repo.

```bash
keytool -genkeypair -v -keystore ~/keys/wakku-release.jks -alias wakku -keyalg RSA -keysize 4096 -validity 36500
```

Then create `keystore.properties` in the repo root. Git ignores it, so it
is never committed:

```properties
storeFile=C:/Users/<user>/keys/wakku-release.jks
storePassword=...
keyAlias=wakku
keyPassword=...
```

Build with `gradle assembleRelease`. The signed APK ends up at
`app/build/outputs/apk/release/app-release.apk`. Without
`keystore.properties`, the release build still succeeds but the APK is
unsigned (`app-release-unsigned.apk`).

## Tests

```bash
gradle testDebugUnitTest
```

The unit tests cover shake detection and alarm timing math. The
instrumented Compose UI tests live in `app/src/androidTest/`. See
`CLAUDE.md` for how to run them on a MIUI device.

## Versioning

[Semantic Versioning](https://semver.org/). To release:

1. Bump `versionName` in `app/build.gradle.kts`, and raise `versionCode`
   by one.
2. Move the `[Unreleased]` entries in `CHANGELOG.md` under the new version.
3. Tag the commit `vX.Y.Z`, then build the signed release APK (see above).

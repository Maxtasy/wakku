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

Wakku isn't on the Play Store. Release APKs are signed and shared directly
(sideloaded). Android only installs an update over an existing copy if both
were signed with the same key. So the release keystore and its password must
never be lost. They're backed up in KeePassXC (keystore file attached to the
entry).

### One-time setup (new machine)

1. Restore `wakku-release.jks` from the KeePassXC entry to
   `~/keys/wakku-release.jks`, outside the repo. (Don't create a new key: an
   APK signed with a new key can't update installs signed with the old one.)
2. Copy `keystore.properties.example` to `keystore.properties` in the repo
   root and fill in the path and password. `keystore.properties`, `*.jks`
   and `*.keystore` are git-ignored, so neither the key nor the password can
   be committed by accident.

Without `keystore.properties`, `gradle assembleRelease` still succeeds, but
the APK it produces is unsigned (`app-release-unsigned.apk`) and can't be
installed.

For reference, the keystore was originally created with:

```bash
"/c/Program Files/Android/Android Studio/jbr/bin/keytool" -genkeypair -v -keystore ~/keys/wakku-release.jks -alias wakku -keyalg RSA -keysize 4096 -validity 36500
```

### Installing a release APK

Send `app-release.apk` any way you like (messenger, email, cloud link).
On the phone, open it, allow "Install unknown apps" for the app it was opened
from when Android asks, and tap through a possible Play Protect warning.
Installing a newer APK over an old one keeps the alarms and settings.

A phone running a debug build (installed from Android Studio or
`assembleDebug`) can't be updated with the release APK, because the signing
keys differ. It has to uninstall first, which deletes its alarms.

## Tests

```bash
gradle testDebugUnitTest
```

The unit tests cover shake detection and alarm timing math. The
instrumented Compose UI tests live in `app/src/androidTest/`. See
`CLAUDE.md` for how to run them on a MIUI device.

## Releasing a new version

Versions follow [Semantic Versioning](https://semver.org/)
(`MAJOR.MINOR.PATCH`): PATCH for bug fixes, MINOR for new features or
visible changes, MAJOR for breaking changes (e.g. a data reset).

1. In `app/build.gradle.kts`, set `versionName` to the new version and
   raise `versionCode` by one. Android refuses to install an APK over one
   with a higher or equal `versionCode`.
2. In `CHANGELOG.md`, move the `[Unreleased]` entries under a new
   `[X.Y.Z]` heading (leave an empty `[Unreleased]` above it), and update
   "Current version" at the top of this README.
3. Run the unit tests (`gradle testDebugUnitTest`).
4. Commit, tag and push:
   ```bash
   git commit -am "vX.Y.Z: <summary>"
   git tag -a vX.Y.Z -m "Wakku X.Y.Z"
   git push origin main vX.Y.Z
   ```
5. Build the signed APK with `gradle assembleRelease`. The output is
   `app/build/outputs/apk/release/app-release.apk`. Optionally, check the
   signature:
   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
   (`apksigner` is in the Android SDK's `build-tools/<version>/`.) The
   certificate's SHA-256 digest must match the release key's:
   `c4cc692536729fdf78c938a0e4025e733906ee252d87a876d8baf3cdd5e976ab`.
6. Send the APK to everyone who has the app (see "Installing a release
   APK" above).

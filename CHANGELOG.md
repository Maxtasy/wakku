# Changelog

All notable changes to Wakku are listed here. The project follows
[Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`, mirrored in
`versionName` in `app/build.gradle.kts`. `versionCode` goes up by one with
every release, whatever kind of release it is.

## [Unreleased]

## [1.1.0]

### Changed
- New dark color theme, shared with the Expense Tracker app (indigo accent,
  near-black background). The app is now dark-only.
- Saving an alarm always turns it on, including one that was switched off.
- The battery-optimization warning banner is now amber.
- New app icon: a shaking bell in the Expense Tracker logo's colors
  (indigo, green, sky blue). It also works as an Android 13+ themed icon,
  and the status-bar/notification icon is now the same bell.

### Added
- Tapping the large time on the alarm edit screen opens the time picker
  (the "Change time" button is still there too).

## [1.0.0]

First version used as a daily alarm (the two-week dogfood run).

- Alarm list with create, edit, delete and on/off toggle; one-time and
  repeating alarms.
- Exact scheduling via `AlarmManager.setAlarmClock()`, restored after a reboot.
- Full-screen ringing screen with sound and vibration.
- Shake-to-stop: tapping Stop silences the alarm and starts the shake
  challenge. If you don't finish it, the alarm snoozes.
- Snooze, including stopping a snoozed alarm early from the notification.
- Persistent "Next alarm" notification.
- Global settings (snooze length, shakes to stop, vibration, sound) with
  per-alarm overrides.
- Battery-optimization exemption prompt.

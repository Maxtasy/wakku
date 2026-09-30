# Changelog

All notable changes to Wakku are listed here. The project follows
[Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`, mirrored in
`versionName` in `app/build.gradle.kts`. `versionCode` goes up by one with
every release, whatever kind of release it is.

## [Unreleased]

## [1.2.0]

### Added
- After saving or switching on an alarm, a short message shows how long until
  it rings, e.g. "Rings in 7 h 32 min".
- German translation. All text now lives in `strings.xml`; the app follows the
  phone's language.
- Gradual volume: the alarm starts quiet and gets louder over 45 seconds. It is
  a global setting and can be overridden per alarm.
- "Skip next" on the "Next alarm" notification: skips only the next ring of a
  repeating alarm.

### Changed
- New app icon: the Wakku mark from the Maxtasy design system (vibration arcs
  around a dot). The status-bar/notification icon matches.

### Fixed
- A snooze (or skip) that was pending when the phone restarted or the app was
  restarted now keeps its time instead of falling back to the alarm's regular
  time.

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

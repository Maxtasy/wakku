# Wakku

An Android alarm clock you have to shake off. Free, ad-free, no accounts —
everything stays on-device.

## Status

Milestone 0 — project setup. The app is a blank Compose screen; alarm
creation, scheduling, ringing, and shake-to-stop land in the milestones that
follow.

## Opening the project

This project has no Java/Android SDK installed in the environment it was
scaffolded in, so it hasn't been built or run yet. To open it:

1. Install [Android Studio](https://developer.android.com/studio) (it bundles
   the JDK, Gradle, and the Android SDK).
2. Open this folder in Android Studio and let it sync.
3. On first sync, Android Studio will notice `gradlew`, `gradlew.bat`, and
   `gradle/wrapper/gradle-wrapper.jar` are missing and offer to generate
   them from `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9). Accept
   that, then commit the generated files so the wrapper is checked in like
   any other Gradle project.
4. Run the `app` configuration on an emulator or a real device.

## Tech stack

Kotlin, Jetpack Compose, MVVM. See the project blueprint for the full stack
rationale, scope, and milestone list.

- **Package**: `com.maxtasy.wakku`
- **Min SDK**: 26 (Android 8.0)

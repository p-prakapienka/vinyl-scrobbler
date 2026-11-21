# Vinyl Scrobbler

Vinyl Scrobbler is a Kotlin Multiplatform application for recognising music
played from a vinyl record and submitting eligible listens to Last.fm.

The initial targets are Android and Desktop/JVM, with Windows as the first
desktop platform. Compose Multiplatform shares the UI and application logic.

## Current milestone

Milestone 1 establishes:

- Android and Desktop build targets.
- AcoustID request and response mapping.
- Provider-neutral recognition contracts.
- The Last.fm listening eligibility policy.
- Tests for API mapping and scrobbling thresholds.

Chromaprint generation and microphone capture are intentionally deferred to the
recognition spike. AcoustID expects a Chromaprint fingerprint and the duration
of the complete source recording; the spike must prove that partial microphone
captures from vinyl are reliable enough before the app is built around them.

## Build

Use JDK 17 and Gradle 9.3.1:

```shell
gradle :composeApp:desktopRun
gradle :composeApp:desktopTest
gradle :androidApp:assembleDebug
```

An AcoustID application API key will be required when live lookup is wired into
the application. Do not commit API keys or Last.fm secrets.

## Also planned

- A personal on-device catalog of the owned records, used when AcoustID misses.
  The collection is small, so a simple fingerprint match is enough.
- A GitHub Actions pipeline that uploads a sideloadable Android APK and a
  Windows MSI, and attaches both to a GitHub Release on `v*` tags.

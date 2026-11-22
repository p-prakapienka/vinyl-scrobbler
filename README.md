# Vinyl Scrobbler

Vinyl Scrobbler is a Kotlin Multiplatform application for recognising music
played from a vinyl record and submitting eligible listens to Last.fm.

The initial targets are Android and Desktop/JVM, with Windows as the first
desktop platform. Compose Multiplatform shares the UI and application logic.

## Status

Milestone 3 is done. The app listens, asks AcoustID when a key is typed in,
and falls back to an on-device catalog of fingerprints from the copies you
own. A miss can be saved. The spike notes are in [SPIKE.md](SPIKE.md). How
this compares with other vinyl scrobblers is in [COMPETITION.md](COMPETITION.md).
Next is Milestone 4, playback tracking. Why Shazam survives a room recording
is parked in [SHAZAM.md](SHAZAM.md) until after the MVP.

Done so far:

- Android and Desktop build targets, including microphone capture.
- Chromaprint fingerprints, checked against Chromaprint's own vectors.
- AcoustID request and response mapping.
- Provider-neutral recognition contracts.
- The Last.fm listening eligibility policy, with tests.
- An on-device catalog of owned tracks. Raw audio is not stored.
- A sideloadable debug APK and a Windows MSI from GitHub Actions.

## Build

Use JDK 17 and Gradle 9.3.1:

```shell
gradle :composeApp:desktopRun
gradle :composeApp:desktopTest
gradle :androidApp:assembleDebug
```

Pushes run [GitHub Actions](https://github.com/p-prakapienka/vinyl-scrobbler/actions). Each run uploads two artifacts and does not publish a Release:

- `vinyl-scrobbler-<version>-<build>.apk` — the sideloadable Android APK, downloaded as itself rather than inside a zip
- `windows-msi` — Windows installer

The debug APK is signed with a key stored in the repo (`androidApp/vinyl-scrobbler-debug.keystore`, password `android`). Every build uses that same key, and CI raises the version number, so a new APK installs over the one already on the phone. App data stays.

The APK from the first pipeline run was signed with a throwaway key. Uninstall that one once, then install the new APK. Later builds update in place.

Live AcoustID lookup uses the key typed into the app. Do not commit API keys or Last.fm secrets.

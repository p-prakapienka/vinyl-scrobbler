# Vinyl Scrobbler

Vinyl Scrobbler is a Kotlin Multiplatform application for recognising music
played from a vinyl record and submitting eligible listens to Last.fm.

The initial targets are Android and Desktop/JVM, with Windows as the first
desktop platform. Compose Multiplatform shares the UI and application logic.

## Status

Milestone 1 and the build pipeline are done. Next is Milestone 2, the AcoustID
recognition spike. Chromaprint generation and microphone capture are not built
yet. The rest of the roadmap is in [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

Done so far:

- Android and Desktop build targets.
- AcoustID request and response mapping.
- Provider-neutral recognition contracts.
- The Last.fm listening eligibility policy, with tests.
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

An AcoustID application API key will be required when live lookup is wired into
the application. Do not commit API keys or Last.fm secrets.

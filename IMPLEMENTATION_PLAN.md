# Implementation plan

## Milestone 1 — Project foundation

- Create Android and Desktop/Windows targets.
- Introduce provider-neutral recognition models and services.
- Implement AcoustID HTTP lookup and response mapping.
- Implement and test Last.fm eligibility rules.

## Build pipeline

Land this as soon as the Gradle targets can package. It does not wait on
recognition. A build has to be installable on a phone and on Windows while the
later milestones are still open.

Follow the same shape as kartka and tx-one-control:

- On every push, GitHub Actions runs the tests and uploads a downloadable
  artifact from the workflow run.
- Android artifact: a debug APK that can be sideloaded. A stable debug
  keystore may live in Actions secrets so the next APK installs over the
  previous one. Never commit the keystore.
- Desktop artifact: the Windows MSI already declared by
  `compose.desktop.nativeDistributions`. Build it on a Windows runner. Deb and
  Dmg stay available locally; they are not the first published artifact.
- On a `v*` tag, attach the APK and the MSI to a GitHub Release.
- Do not commit AcoustID keys, Last.fm secrets, or signing keys.

## Milestone 2 — AcoustID recognition spike

- Capture PCM microphone audio on Android and Desktop.
- Integrate Chromaprint behind `AudioFingerprinter`.
- Record controlled samples from at least five vinyl records.
- Test beginning-of-track and mid-track captures at several sample lengths.
- Record match rate, latency, CPU use, and false positives.
- Decide whether AcoustID is viable for continuous recognition.

This milestone is a decision gate. AcoustID requires the duration of the whole
source audio and is primarily designed for file fingerprint lookup. If
mid-track microphone samples are unreliable, keep AcoustID for the cases it
does match and use the personal collection database in Milestone 3 as the
fallback. Do not block the product on ShazamKit or another commercial
recogniser.

## Milestone 3 — Personal collection database

The collection is small, so a private catalog of the records actually owned is
an acceptable fallback when AcoustID misses, is low-confidence, or cannot be
reached.

- Store each owned track on device: artist, album, title, duration, and a
  fingerprint (or a few fingerprints) taken from that copy.
- Do not keep raw microphone audio. Fingerprints and metadata only.
- Try AcoustID first. Search the local catalog only when AcoustID returns
  nothing useful. A simple comparison is enough; do not build a general music
  index.
- Let a failed recognition become a catalog entry: the user confirms the
  metadata, the fingerprint is kept, and the next play can match locally.
- Return local hits through the same recognition interface. Playback tracking
  must not care whether a candidate came from AcoustID or the catalog.
- Test a hit, a miss, a low-confidence AcoustID result that falls through, and
  adding a track, against a fixture catalog.

## Milestone 4 — Playback tracking

- Implement a stateful `PlaybackTracker`.
- Require repeated recognition before accepting a candidate.
- Detect track transitions, replay, silence, and loss of recognition.
- Track only listening time observed by the application.
- Add deterministic state-machine tests.

## Milestone 5 — Last.fm integration

- Implement Last.fm authentication.
- Send Now Playing updates after confirmed recognition.
- Submit a scrobble after half the track or four minutes, whichever comes first.
- Preserve the inferred track-start timestamp.
- Handle rejected and corrected metadata explicitly.

## Milestone 6 — Reliability

- Persist queued scrobbles across application restarts.
- Retry transient Last.fm failures in order.
- Prevent duplicate submissions.
- Add diagnostics that do not retain raw microphone audio.

## Milestone 7 — Background operation

- Add an Android foreground service and persistent microphone indicator.
- Add desktop tray controls and microphone selection.
- Surface recognised track, confidence, elapsed time, and submission status.

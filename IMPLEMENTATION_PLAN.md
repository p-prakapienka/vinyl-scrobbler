# Implementation plan

Progress is tracked here. Done items are checked. There is no issue tracker.

| | Status |
| --- | --- |
| Milestone 1 — Project foundation | Done |
| Build pipeline | Done |
| Milestone 2 — AcoustID recognition spike | Done |
| Milestone 3 — Personal collection database | Next |

## Milestone 1 — Project foundation

- [x] Create Android and Desktop/Windows targets.
- [x] Introduce provider-neutral recognition models and services.
- [x] Implement AcoustID HTTP lookup and response mapping.
- [x] Implement and test Last.fm eligibility rules.

## Build pipeline

Done. A build is installable on a phone and on Windows while the later
milestones are still open.

Follow the same shape as the CI artifacts in kartka and tx-one-control. Those
repos do not publish a GitHub Release for ordinary builds; the download is the
workflow artifact. androtone is the exception, and only once a version was
worth tagging.

- [x] On every push, GitHub Actions runs the tests and uploads a downloadable
  artifact from the workflow run.
- [x] Android artifact: a debug APK signed with the checked-in sideload keystore.
  CI passes `-Pvinyl.versionCode` from the Actions run number, so a new APK
  installs over the previous one and keeps app data. Do not commit a release
  keystore.
- [x] Desktop artifact: the Windows MSI already declared by
  `compose.desktop.nativeDistributions`. Build it on a Windows runner. Deb and
  Dmg stay available locally; they are not the first published artifact.
- [x] No GitHub Release at this stage. A `v*` release can wait until there is a
  version worth publishing.
- [x] Do not commit AcoustID keys, Last.fm secrets, or release signing keys.

## Milestone 2 — AcoustID recognition spike

- [x] Capture PCM microphone audio on Android and Desktop.
- [x] Integrate Chromaprint behind `AudioFingerprinter`.
- [x] Compare beginning-of-track and mid-track slices at 5, 10, and 20 seconds
  on five controlled recordings. The numbers are in [SPIKE.md](SPIKE.md).
  There was no turntable here; Listen in the app repeats the cut on a live take.
- [x] Record match behaviour, latency, CPU use, and false positives.
- [x] Decide whether AcoustID is viable for continuous recognition.

AcoustID stays the first lookup. Partial fingerprints are faithful slices of
the full take and take well under 50 ms for 30 seconds of audio, so a clean
excerpt is worth sending. It is not enough on its own for continuous vinyl:
this run did not hear a record, and a miss still needs the catalog in
Milestone 3. Do not block the product on another commercial recogniser.

## Milestone 3 — Personal collection database

The collection is small, so a private catalog of the records actually owned is
an acceptable fallback when AcoustID misses, is low-confidence, or cannot be
reached.

- [ ] Store each owned track on device: artist, album, title, duration, and a
  fingerprint (or a few fingerprints) taken from that copy.
- [ ] Do not keep raw microphone audio. Fingerprints and metadata only.
- [ ] Try AcoustID first. Search the local catalog only when AcoustID returns
  nothing useful. A simple comparison is enough; do not build a general music
  index.
- [ ] Let a failed recognition become a catalog entry: the user confirms the
  metadata, the fingerprint is kept, and the next play can match locally.
- [ ] Return local hits through the same recognition interface. Playback tracking
  must not care whether a candidate came from AcoustID or the catalog.
- [ ] Test a hit, a miss, a low-confidence AcoustID result that falls through, and
  adding a track, against a fixture catalog.

## Milestone 4 — Playback tracking

- [ ] Implement a stateful `PlaybackTracker`.
- [ ] Require repeated recognition before accepting a candidate.
- [ ] Detect track transitions, replay, silence, and loss of recognition.
- [ ] Track only listening time observed by the application.
- [ ] Add deterministic state-machine tests.

## Milestone 5 — Last.fm integration

- [ ] Implement Last.fm authentication.
- [ ] Send Now Playing updates after confirmed recognition.
- [ ] Submit a scrobble after half the track or four minutes, whichever comes first.
- [ ] Preserve the inferred track-start timestamp.
- [ ] Handle rejected and corrected metadata explicitly.

## Milestone 6 — Reliability

- [ ] Persist queued scrobbles across application restarts.
- [ ] Retry transient Last.fm failures in order.
- [ ] Prevent duplicate submissions.
- [ ] Add diagnostics that do not retain raw microphone audio.

## Milestone 7 — Background operation

- [ ] Add an Android foreground service and persistent microphone indicator.
- [ ] Add desktop tray controls and microphone selection.
- [ ] Surface recognised track, confidence, elapsed time, and submission status.

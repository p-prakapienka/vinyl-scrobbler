# Implementation plan

## Milestone 1 — Project foundation

- Create Android and Desktop/Windows targets.
- Introduce provider-neutral recognition models and services.
- Implement AcoustID HTTP lookup and response mapping.
- Implement and test Last.fm eligibility rules.

## Milestone 2 — AcoustID recognition spike

- Capture PCM microphone audio on Android and Desktop.
- Integrate Chromaprint behind `AudioFingerprinter`.
- Record controlled samples from at least five vinyl records.
- Test beginning-of-track and mid-track captures at several sample lengths.
- Record match rate, latency, CPU use, and false positives.
- Decide whether AcoustID is viable for continuous recognition.

This milestone is a decision gate. AcoustID requires the duration of the whole
source audio and is primarily designed for file fingerprint lookup. If mid-track
microphone samples are unreliable, retain the recognition abstraction and move
to ShazamKit on Android plus a desktop-capable provider.

## Milestone 3 — Playback tracking

- Implement a stateful `PlaybackTracker`.
- Require repeated recognition before accepting a candidate.
- Detect track transitions, replay, silence, and loss of recognition.
- Track only listening time observed by the application.
- Add deterministic state-machine tests.

## Milestone 4 — Last.fm integration

- Implement Last.fm authentication.
- Send Now Playing updates after confirmed recognition.
- Submit a scrobble after half the track or four minutes, whichever comes first.
- Preserve the inferred track-start timestamp.
- Handle rejected and corrected metadata explicitly.

## Milestone 5 — Reliability

- Persist queued scrobbles across application restarts.
- Retry transient Last.fm failures in order.
- Prevent duplicate submissions.
- Add diagnostics that do not retain raw microphone audio.

## Milestone 6 — Background operation

- Add an Android foreground service and persistent microphone indicator.
- Add desktop tray controls and microphone selection.
- Surface recognised track, confidence, elapsed time, and submission status.

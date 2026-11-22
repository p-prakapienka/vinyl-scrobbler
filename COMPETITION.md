# Competition

Reviewed 2026-11-22. Public projects that scrobble vinyl fall into two
families. This one is in the second. The phone and desktop app is the better
product shape. It is not yet the better listener.

## Discogs, then a timer

You look up the release. The app walks the tracklist by duration and submits
to Last.fm. Nothing listens.

| Project | What it is |
| --- | --- |
| [tjol/vinylscrobbler](https://github.com/tjol/vinylscrobbler) | 2011 Android app. Discogs collection or search, then submit that record. The original name collision. Abandoned stack. |
| [russmckendrick/vinyl-scrobbler](https://github.com/russmckendrick/vinyl-scrobbler) | macOS menu-bar app. Discogs release id, now playing, per-track countdown. |
| [nesticle8bit/vinyl-scrobbler](https://github.com/nesticle8bit/vinyl-scrobbler) | Node CLI. One Discogs release, backdated Last.fm scrobbles. |
| [s-lindenau/OpenMusicScrobbler](https://github.com/s-lindenau/OpenMusicScrobbler) | Desktop. Browse a Discogs collection and scrobble the chosen tracks. |
| [elamperti/OpenWebScrobbler](https://github.com/elamperti/OpenWebScrobbler) | General manual web scrobbler. Vinyl is one use. Optional Discogs. |
| [baasman/pyvinyl](https://github.com/baasman/pyvinyl) | Flask collection, manual scrobble, local stats. |
| [vinylscrobbler.com](https://vinylscrobbler.com) | Hosted version of the same idea. |

These are too basic for this project. They never hear the record, so a replay,
a skip, or the wrong side is still a scrobble. They are useful only as a
reminder that Discogs already has the tracklist and the durations.

## Listen, then identify

Same goal as this project. The ones that run are a Raspberry Pi or a browser,
and the ones that stay on AcoustID never shipped.

| Project | Status | Recogniser | What it actually does |
| --- | --- | --- | --- |
| [banool/vinyl-scrobbler](https://github.com/banool/vinyl-scrobbler) | README only | AcoustID, planned | Per-track identification, 20 s samples, majority vote, modular source. Asks what happens to tracks shorter than the sample. No code. |
| [alastair/scrobbyl](https://github.com/alastair/scrobbyl) | Proof of concept, abandoned | Echo Nest, shut down | Line-in, 20 s chunks, scrobble when the chunk changes. Microphone was left as future work. Not for a room. |
| [powerpcme/VinylPi](https://github.com/powerpcme/VinylPi) | Runs on a Pi | Shazam | Three captures of 3 s, accept on 2 of 3. Stereo line-in required. A mono mic input is documented as broken. Obscure records miss. |
| [zamudioo/VinylScrobbler](https://github.com/zamudioo/VinylScrobbler) | Runs on a Pi | Shazam | USB turntable, splitter, or mic. Last.fm, SQLite stats, a page on the LAN. No fingerprint catalog. |
| [potseeslc/vinylbuddy](https://github.com/potseeslc/vinylbuddy) | Self-hosted web app | Shazam in the browser | Continuous listen, artwork, optional Last.fm. The mic works only on localhost or HTTPS. |

[connerjc234/vinylripper](https://github.com/connerjc234/vinylripper) is not
a scrobbler. It records line-in, splits on silence, and tags from Discogs.

## Why this one is the better fit

The timer apps need you to drive them. The listeners that work need a Pi,
Docker, a browser tab, a line splitter, or a commercial API, and they still
miss pressings Shazam does not know. This project is the app on the Android
phone and the Windows machine you already open.

- Recognition is a seam. Playback tracking does not care whether a candidate
  came from AcoustID or the on-device catalog. The Shazam apps hard-wire one
  provider.
- Chromaprint is in the app, shared by both targets, and checked against
  Chromaprint's own vectors. Nothing here shells out to a native fingerprinter
  or to a service that can disappear the way Echo Nest did.
- The catalog in Milestone 3 is the fallback those READMEs ask for and do not
  build: fingerprints of the copies you own, kept without the raw audio.
  Chromaprint is built for near-identical audio. Matching a fingerprint taken
  from this pressing is inside that design. Querying the public AcoustID index
  with a short room recording is not, which is why [SPIKE.md](SPIKE.md) keeps
  AcoustID as the first lookup and not the only one.
- Milestone 4 asks for a repeated confirmation, then transitions, replay,
  silence, and loss of recognition. VinylPi's 2-of-3 vote is the only
  confirmation logic that exists in running code. A timer app cannot see a
  replay at all.

## Where it is not better

None of this has been tried on a record. The spike used synthetic tones. The
working listeners cheated the room by tapping the turntable, then paid
Shazam, and they do scrobble today. This app does not.

- No Discogs import. Every maintained vinyl scrobbler takes titles and
  durations from Discogs. Here they are typed after a miss. The
  half-or-four-minutes rule needs a duration.
- No album-locked mode. Once a timer app knows the release, it stops
  identifying. Continuous identification is right for an unknown stack. It is
  more work than a known shelf requires.
- The capture path is a microphone. VinylPi says a mono mic input breaks
  detection. Scrobbyl required line-in. That gap is still open.
- Confirmation, the scrobble queue, and background listening are still later
  milestones. A phone listener dies in the background until the foreground
  service in Milestone 7 exists. The Pi apps just stay up.
- Tracks shorter than the recognition window are unsolved. banool already
  named that case. A 5 s slice is only 19 subfingerprints.
- There is no local listening history. zamudioo keeps one for when Last.fm
  rejects a scrobble. Milestone 6 persists the queue and stops there.

## What this does not change

Do not add a Pi, a web frontend, or another commercial recogniser to copy the
projects that run. Discogs as a metadata source can wait; it does not change
the product. The open risk is the microphone in the room, which Milestone 3
is for. Why that room still identifies on Shazam, and what of that is worth
retrying after the MVP, is in [SHAZAM.md](SHAZAM.md).

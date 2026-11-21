# Milestone 2 spike

Chromaprint algorithm 2, checked against the Chromaprint 1.6 test vectors:

- Silence, 130 × 1024 zero samples at 44100 Hz mono, becomes
  `AQAAA0mUaEkSRZEGAA` (3 identical subfingerprints).
- A 12 second two-tone signal at 11025 Hz matches the fingerprint produced by
  that same Chromaprint build.
- Raw fingerprint compression matches Chromaprint, including `NwAAAkEA` for
  the `[1, 0]` vector.

The fingerprinter runs at 11025 Hz. Android and desktop ask the microphone for
that rate first. Other integer multiples are averaged down. Anything else is
linearly resampled. A 44100 Hz music capture is therefore not bit-identical to
`fpcalc`, which uses a different resampler. Zeros are.

## Controlled partial captures

Five 30 second sources at 11025 Hz, each a different six-note melody. No
turntable was available, so these are not vinyl. The app runs the same
start/middle cut on a live listen.

| Slice | Subfingerprints | Relationship to the full fingerprint |
| --- | ---: | --- |
| 30 s, whole take | 221 | the reference |
| first 5 s | 19 | prefix |
| first 10 s | 59 | prefix |
| first 20 s | 140 | prefix |
| 5, 10, and 20 s from 8 s in, aligned to a frame | same lengths | contiguous match at the frame the slice started on |

That held for all five sources. A 10 second slice from one source was not a
contiguous match inside any of the other four (0 of 20).

Fingerprinting one 30 second take took 25–44 ms on the desktop test runner,
single threaded. That is roughly 700× realtime, so CPU is not the constraint.

## Decision

AcoustID stays the first lookup. A partial capture is a real slice of the
fingerprint of the whole take, and it is cheap to compute, so a clean excerpt
of about 10 seconds or more is a fair query when the recording is in the
database.

It is not viable as the only recogniser for continuous vinyl listening. This
run did not measure a microphone in front of a record, and Chromaprint is built
for full files and long streams rather than noisy short excerpts. A miss, a
weak score, or a wrong hit should fall through to the on-device catalog in
Milestone 3. The lookup itself runs from the app when an AcoustID application
key is typed in. The key is not stored and is not in the repo.

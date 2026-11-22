# Shazam

Parked. Reviewed 2026-11-22. Come back after the MVP. Do not take a Shazam
dependency, and do not change Milestone 3 for this. The on-device catalog
stays Chromaprint.

This note is why a room recording still identifies on Shazam, and which part
of that is worth retrying once the catalog and a real turntable exist.

## Chromaprint, as used here

Chromaprint algorithm 2 is the fingerprint behind AcoustID and behind
`fpcalc`. The app implements it in `ChromaprintFingerprinter` and checks it
against Chromaprint 1.6's own vectors. The numbers are in [SPIKE.md](SPIKE.md).

Audio is mixed to mono and resampled to 11025 Hz. Overlapping FFTs are folded
into chroma bands, filtered over time, and packed into 32-bit subfingerprints,
about eight a second once the filter has filled. The sequence is compressed
to the base64 string AcoustID accepts, with the duration.

It is built for near-identical audio: the same take after compression, small
EQ, or light noise. A fingerprint taken from this pressing is inside that
design. A short room recording, queried against the public index of clean
rips, is not.

## How Shazam matches

The published algorithm is Avery Li-Chun Wang, "An Industrial-Strength Audio
Search Algorithm" (ISMIR 2003). A few seconds of audio is the query. The
index is the clean master, not the recording in the room.

1. Keep spectrogram peaks only. Local maxima become a sparse constellation of
   time and frequency. Amplitude is discarded. Noise fills the valleys. EQ
   and a microphone's response change levels. A snare, a vocal formant, or a
   strong harmonic stays put.
2. Hash pairs, not points. Each peak is an anchor and is paired with several
   peaks in a short window ahead of it. The hash is `(f_anchor, f_target, Δt)`,
   packed into a 32-bit integer. `Δt` is relative, so the clip can start
   anywhere in the track.
3. Store each hash with the track id and the anchor's position in the master.
   The query only has to regenerate some of the same hashes.
4. Votes have to agree on one offset. Every hit votes for `t_master − t_query`.
   A real match piles up at one offset. Accidental collisions scatter. The
   histogram peak is the identification, and it is also the position in the
   track.

The paper measured recognition below −6 dB SNR for white noise and −9 dB for
structured noise such as a voiceover, including traffic, dropouts, and other
music mixed on top. Pairing costs survival: if a peak survives with
probability `p`, a pair survives with about `p²`. Most pairs can die. The
ones that remain still vote together.

## Why the room kills Chromaprint and not this

Chromaprint quantizes the whole chroma shape into a dense bit string and asks
for that string to be nearly the same. Reverb, rumble, and surface noise flip
those bits. A 5 s slice is only 19 subfingerprints, so there is little left
to agree on.

Shazam never needed the bits to match. It needed a minority of peak pairs to
survive and vote for one offset. The catalog entry is the clean master, so
the room never has to resemble the file.

## What Shazam still gets wrong for vinyl

- The master has to be in the catalog. An obscure pressing, a private press,
  or anything never digitized is a miss. VinylPi already reports that.
- `Δt` and the two frequencies are exact. A turntable running fast or slow
  scales every time gap and shifts every peak, so the hashes miss. Wow smears
  the offset peak.
- A different edit or remaster can lock onto the catalog version rather than
  the copy on the platter.
- It is a short query against a commercial index. Audio, or a fingerprint
  only their client can use, leaves the device. VinylPi's 2-of-3 vote on
  three captures of 3 s is as continuous as the working listeners get.
- The service can disappear or start charging. Echo Nest already did the
  first. Do not put it behind `MusicRecognitionService`.

## What to retry after the MVP

Not a Shazam API. The question is whether the on-device catalog should match
with landmark hashes instead of, or as well as, Chromaprint.

The reference would be this pressing, captured the same way as the query, and
the query is a room microphone. That is the case Wang's peaks were built for,
and it is a stronger fit for a noisy room than a chroma bit string is. The
collection is small, so the comparison can stay a direct search of owned
tracks. Do not build a general music index. The same rule is already in
Milestone 3.

Speed drift still breaks an exact `Δt`. Try the catalog with Chromaprint
first, on a real record, and only then decide whether peaks earn a second
fingerprinter. Raw audio is still not stored. Fingerprints and metadata only.

# Galileo OSNMA

GPSTest can verify Galileo Open Service Navigation Message Authentication
([OSNMA](https://www.gsc-europa.eu/galileo/services/galileo-open-service-navigation-message-authentication-osnma)).
OSNMA lets a receiver check that the ephemeris, clock and health data broadcast by Galileo
satellites really comes from Galileo and hasn't been forged by a spoofer.

GPSTest does all the cryptography on the phone, from the Galileo E1-B I/NAV navigation messages
that Android delivers to apps. The result is shown on the **Status** screen.

<img src="docs/osnma-pixel8pro.webp" alt="Galileo OSNMA card on a Pixel 8 Pro, expanded: 4 Galileo satellites authenticated">

*The expanded OSNMA card on a Pixel 8 Pro (Android 17): 4 Galileo satellites authenticated,
starting from the TESLA key of the previous session, with the shifted bit layout of this device
detected and the time checked against network time.*

![Galileo OSNMA card states in the dark theme (mockup)](docs/osnma-card-dark.png)

*Mockup of the card in the dark theme: expanded while authenticating (numbers from a Pixel 8 Pro),
expanded while the root key is still being collected, and the collapsed one-line states.*

![Status screen with the collapsed Galileo OSNMA card and sealed satellites (mockup)](docs/osnma-screen-dark.png)

*Mockup of the Status screen: the collapsed card with the number of authenticated and failing
satellites, and seals next to the Galileo satellites in the list (green: authenticated, red: a
tag failed in the last 10 minutes).*

## Requirements

- A device that provides Galileo navigation messages to apps (`GnssNavigationMessage` of type
  `TYPE_GAL_I`). Many don't. Check "Navigation Messages" under *What GNSS features does my
  device support?* in the [FAQ](FAQ.md). The OSNMA card appears once such messages arrive, and right away on later starts.
- Galileo satellites in view, ideally with a clear sky. Authentication starts after a few
  minutes of continuous reception.
- The correct time: within 15 seconds for all tags, within 165 seconds for slow MAC tags only.
  GPSTest uses Google TrustedTime (Google Play build) or Android's network time (Android 13 and
  later) where available, otherwise the device clock. See [Time check](#time-check).

## The Status card

| Field | Meaning |
| --- | --- |
| NMA status | Operational, Test or Don't use, as broadcast by Galileo. A ✓ means it was confirmed by a verified signature. |
| Public key | The ECDSA public key ID (PKID), its curve and where it came from: *built in*, *from signal*, *Merkle tree file* or *imported*. |
| TESLA chain | Chain ID, MAC function, tag size and time of the latest verified TESLA key. |
| Authenticated | Satellites whose current ephemeris, clock and health data is authenticated. That means at least 40 bits of MAC tags were verified on the data the satellite is broadcasting right now. |
| Failed tags | Satellites with a tag that failed verification in the last 10 minutes, with the count. |

The card is a single line by default: the main state chip and any warning chips next to it
(on narrow screens, chips that don't fit continue on a second line), the number of authenticated satellites (green seal) and of satellites with failed tags
(red seal), a spinner while authentication is in progress, and a chevron. Tap it to expand: a
sentence on the current state (with how long it usually takes while in progress), the fields
above, tag counts by type (CED, timing,
slow MAC), page counters (received, used, without time, not nominal, CRC errors), device parity,
subframe completeness, page timing, clock offset and the latest events. All fields are shown
from the start, with "Not known yet", "None yet" or the root key progress until they're filled
in, so the layout doesn't jump while authentication progresses.

### States

The card shows exactly one **main state** chip:

| Main state | Meaning |
| --- | --- |
| **NO DATA** (gray) | No Galileo pages yet, or no OSNMA data in them. |
| **IN PROGRESS** (amber) | Synchronizing with Galileo time, waiting for the public key, the TESLA root key or the first tags. A spinner shows that pages are arriving. |
| **AUTHENTICATED** (green) | At least one satellite's navigation data is authenticated. |
| **TIME NOT VERIFIED** (red) | The time is more than 165 s off; see below. |
| **DON'T USE** (red) | Galileo set the OSNMA status to "don't use". |
| **ALERT** (red) | An OSNMA alert message revoked all keys. |

Next to the main state chip, a chip is added for each **warning** that applies. They can occur in any
combination, and together with any main state:

| Warning | Meaning |
| --- | --- |
| **FAILED TAGS** (red) | A satellite's tag failed in the last 10 minutes. |
| **POOR RECEPTION** (amber) | Too many pages are missing, or they stopped arriving. |
| **SLOW MAC ONLY** (amber) | The time is 15 to 165 s off, so only slow MAC tags are used. |

In the code, these are `OsnmaMainState` and `OsnmaWarning` (`OsnmaCardState.kt`).

### Satellite list and sky view

Galileo satellites are marked in the satellite list on the Status screen (next to the flag) and in
the sky view (at the upper right of the triangle):

- **Green seal with a check mark**: the satellite's ephemeris, clock and health data is
  authenticated.
- **Red seal with an exclamation mark**: a tag of this satellite failed in the last 10 minutes.

Both are also explained in the sky view's legend.

### Warning notification

When a tag fails, GPSTest posts a notification ("Galileo authentication failed") naming the
satellites, at most once every 10 minutes. It has its own notification channel, *Galileo OSNMA
warnings*, so it can be turned off in Android's notification settings without affecting the
tracking notification.

### Red and amber states

- **FAILED TAGS**: a MAC tag didn't match the received navigation data. This can be caused by
  corrupted data or timing problems, but also by spoofing. Treat data from those satellites as
  untrusted.
- **TIME NOT VERIFIED**: the time differs from the Galileo time in the signal by more than
  165 s. OSNMA can't detect replayed or delayed signals without an independent time source, so
  no tags are used until that's fixed. Check the date and time settings. Between 15 and 165 s,
  only slow MAC tags are used: the main chip stays green, a **SLOW MAC ONLY** chip is added, and
  the details have an amber note. See [Time check](#time-check).
- **DON'T USE**: Galileo has set the OSNMA status to "don't use". Nothing is authenticated
  until that changes.
- **POOR RECEPTION** (amber chip and hint in the details): Galileo pages arrive, but fewer than half of the 30-second subframes
  are complete, or the pages stopped while tracking. Each satellite's authentication data needs
  all 15 pages of a subframe, so authentication becomes slow or stops. This is usually the chip
  saving power (duty cycling) or tracking only the E1 pilot signal, which carries no data. Keep
  **Force full GNSS measurements** on (Android 12 and later), keep the screen on, and give the
  phone a clear view of the sky. The share of complete subframes is shown in the details.
- **ALERT**: an authenticated OSNMA Alert Message was received. All keys, including the built-in
  Merkle tree root, are no longer trusted. Import new key material from the GSC to resume.

## Keys

OSNMA needs the Galileo ECDSA public key to verify the TESLA root key. Nothing has to be set up:

1. **Built in.** GPSTest ships with the current Galileo public key (PKID 2) and the Merkle tree
   root. The key is only used after its Merkle tree path is checked against the root, so
   authentication starts within minutes of the first fix.
2. **From the signal.** When Galileo introduces a new key, it broadcasts it every 6 hours (at 00,
   06, 12 and 18 h Galileo time). GPSTest verifies it against the Merkle tree root and stores it,
   so new keys are picked up without an app update.
3. **Imported (optional).** Only needed if the Merkle tree itself changes, or after an alert
   message. Download the files from the
   [European GNSS Service Centre](https://www.gsc-europa.eu/gsc-products/OSNMA/PKI) (free
   registration) and import one under **Settings > Galileo OSNMA > Import OSNMA public key or
   Merkle tree**. Supported files:
   - `OSNMA_MerkleTree_*.xml`: replaces the Merkle tree root. The public key it contains is
     verified against that root before use.
   - `OSNMA_PublicKey_*.xml`
   - `OSNMA_PublicKey_*.crt` or a PEM public key. The PKID is taken from the file name
     (`…PKID_2…`).

**Forget OSNMA keys** deletes the stored public keys, any imported Merkle tree, the saved TESLA
key and the saved parts of the TESLA root key. The built-in key stays.
**Authenticate Galileo navigation data** turns the feature off.

### Time check

TESLA keys are published 30 s after the tags they protect, so a spoofer that can delay the
signal could forge tags if the receiver relied only on the signal for the time. OSNMA therefore
requires the receiver to know Galileo time on its own (OSNMA Receiver Guidelines, sections 2.1
and 5.3.1). Each time a page carries its own time stamp, GPSTest compares it with a reference
time, the first available of:

- **Google TrustedTime** (Google Play build only): Google Play services' time, synchronized with
  Google's time servers and independent of GNSS. The **Clock offset** row says "(Google
  TrustedTime)".
- **Network time** (Android 13 and later, when the phone has synchronized with a time server).
  It's independent of GNSS.
- Otherwise the **device clock**. Some phones set it from GNSS, so it isn't fully independent:
  the **Clock offset** row says "(device clock, which may be set from GNSS)".

| Difference | Tags used |
| --- | --- |
| Up to 15 s | All |
| 15 to 165 s | Slow MAC only (ADKD 12, checked with a key sent 5 minutes later) |
| More than 165 s, or not known yet | None (TESLA keys are still verified) |

When the check gets stricter, what was authenticated with tags no longer allowed is dropped.
Until the first time stamp arrives (within 30 s of reception), no tags are used.

## What is verified

GPSTest follows the
[OSNMA SIS ICD v1.1](https://www.gsc-europa.eu/sites/default/files/sites/all/files/Galileo_OSNMA_SIS_ICD_v1.1.pdf)
and the OSNMA Receiver Guidelines:

- I/NAV page CRC (CRC-24Q)
- DSM-PKR: public keys against the Merkle tree root
- DSM-KROOT: ECDSA P-256/SHA-256 or P-521/SHA-512 signature of the TESLA root key
- TESLA key chain (SHA-256 or SHA3-256), MACSEQ and the MAC look-up table
- MAC tags (HMAC-SHA-256 or CMAC-AES) for ADKD 0 (ephemeris, clock and health), ADKD 4
  (timing parameters) and ADKD 12 (slow MAC)
- Chain renewal and revocation, public key renewal and revocation, alert messages

The engine is in `library/src/main/java/com/android/gpstest/library/osnma`. It's pure Kotlin
without Android or third-party dependencies, and is modelled on the
[galileo-osnma](https://github.com/daniestevez/galileo-osnma) Rust library. It produces the
same results as galileo-osnma, tag for tag, on all
[GSC OSNMA test vector](https://www.gsc-europa.eu/support-to-developers/osnma-test-vectors)
scenarios and on a real receiver recording. The unit tests in
`library/src/test/java/com/android/gpstest/library/osnma` run on a trimmed test vector and on
Galileo messages recorded on a Pixel 8 Pro, a Pixel 10 Pro XL and a Pixel 11 Pro (see *Device
quirks*):

```
./gradlew :library:testDebugUnitTest --tests "com.android.gpstest.library.osnma.*"
```

## Limitations

- **Only the navigation messages are authenticated.** GPSTest can't tell whether the device's
  own position fix used authenticated data. The chipset computes the fix independently.
- **Time comes from the signal.** Android doesn't provide the Galileo time of each page, so
  GPSTest reads it from the I/NAV words and the arrival times. It's checked against an
  independent time where possible (see [Time check](#time-check)). Like any OSNMA receiver
  without a secure time source, it can't detect all replay or delay attacks.
- **Data must be complete.** A satellite is only authenticated once all the words covered by
  its tags have been received, so weak or intermittent signals can delay it.
- **Devices that skip the CRC bits.** If the CRC fails on practically every page while the device
  says they passed its parity check, GPSTest assumes the device doesn't fill in the CRC bits,
  uses the device's check instead and says so in the details. Some devices mark every page as
  passed, damaged or not, so this fallback is only used when no page at all passes the CRC.

## Speeding things up

OSNMA was designed for receivers that see every page. Phones often don't, so GPSTest uses the
OSNMA messages page by page instead of waiting for complete 30 s subframes:

- **TESLA root key assembled across satellites.** All satellites broadcast the same DSM-KROOT
  message. Each of its bytes needs to arrive only once, from any satellite and any repetition.
  The result is accepted only if its ECDSA signature is valid.
- **TESLA keys from several satellites.** All satellites transmit the same TESLA key in a
  subframe, so it's put together from the pages of any of them. When keys of some subframes
  are missing, they're derived from the next verified key, so those subframes' tags aren't lost.
- **Tags from partial MACK messages.** Each tag is checked as soon as its own pages have arrived.
  The MACSEQ, which protects the flexible tags, is checked when its pages are there; without it,
  flexible tags are skipped. Fixed tags are checked against the MAC look-up table as usual.
- **Warm start.** The last verified TESLA key is saved. On the next start, new keys are verified
  by hashing back to it, so authentication can start within about a minute, without waiting for
  the root key. This works for up to a year after the saved key, as long as the same chain is in
  force (hashing back that far takes a few seconds, so it's tried at most every 2.5 minutes).
  The saved key is dropped as soon as the signal shows a different chain in force, a revoked
  chain or key, an alert message, or a "don't use" status, and when keys are forgotten in the
  settings. The details then show the TESLA chain as "(from the previous session)". If the
  root key from the signal arrives, it takes over.
- **Built-in TESLA root key.** GPSTest also ships with a signed root key message (DSM-KROOT) of
  the chain in force, so even the very first session can start without receiving it. It's
  checked like one from the signal: its ECDSA signature must verify with the built-in public
  key, and the same rules as for the warm start drop it. The details then show "(built-in root
  key)". When the chain changes, the app simply falls back to the signal until the built-in
  message is updated. Each root key GPSTest verifies is written to Logcat ("Verified
  DSM-KROOT") and, with OSNMA file logging on, to the CSV file (see [LOGGING.md](LOGGING.md)),
  so it can be built into a later version (`GalileoKeys.kt`).
- **Root key parts kept between sessions.** The parts of the TESLA root key (DSM-KROOT) and of
  public key messages received so far are saved every minute and when tracking stops. The next
  session continues collecting where the last one stopped, which matters on phones that miss
  most pages. Saved parts older than 3 days are ignored. They can't do harm: a message is only
  used once its signature or Merkle tree path checks out, and if the check fails, the saved
  parts are dropped and the message is collected again from the signal.

On the GSC test data with pages dropped at random, authentication still works when most pages
are missing (24 satellites at 30 % loss, 17 satellites at 70 % loss), where complete subframes
practically never occur. Pages whose timing is ambiguous aren't used for OSNMA data, so a page
in the wrong slot can't make a tag fail.

## Device quirks

Real phones don't always deliver Galileo I/NAV pages the way the Android documentation
describes. GPSTest detects and works around these differences. Each one is shown in the
expanded card.

The unit tests replay logs from three phones (Galileo I/NAV messages only):

| | Pixel 8 Pro | Pixel 10 Pro XL | Pixel 11 Pro |
| --- | --- | --- | --- |
| GNSS chip | GNSS hardware year 2020 | Samsung (S.LSI SPOTNAV) | MediaTek (MNL) |
| Log | 18 min, GPSTest | 88 min, GnssLogger | 31 min, GnssLogger |
| Bit layout | Shifted by 4 bits | As documented | As documented |
| Message ID | Subframe number, -1 for a third of the pages | 1 to 30, not the subframe number | Subframe number, by the page's end |
| Pages delivered twice | No | No | Every third page, others at times |
| Result | TESLA root key still incomplete, nothing fails | Root key from the signal, 5 satellites authenticated | 4 satellites authenticated with the built-in root key |

In all three logs, no tag fails and every page passes the CRC.

### Bit layout of `TYPE_GAL_I` data

The
[`GnssNavigationMessage`](https://developer.android.com/reference/android/location/GnssNavigationMessage)
documentation says that a Galileo I/NAV page (2 x 114 bits, sync and tail removed) is packed
into 29 bytes, MSB first, with the 4 unused bits at the end ("skip B229-B232"):

```
documented:  [ 228 page bits ............................. ][ 4 unused ]
Pixel 8 Pro: [ 4 unused ][ 228 page bits ............................. ]
```

A Pixel 8 Pro (Android 17) puts the 4 unused bits at the start instead, so the whole page is
shifted by 4 bits. Read the documented way, almost every field is garbage: the CRC fails, most
pages look "not nominal", and the word type and time stamps decode to nonsense (for example a
week number of 2904 instead of 1418). Read with the 4-bit offset, every page in a recorded log
was nominal and passed the CRC. Reported to Google as
[issue 580675649](https://issuetracker.google.com/issues/580675649).

`InavLayoutDetector` checks each message with both layouts and uses the one under which the
page passes the CRC. Once a few pages pass with one layout and none with the other, that layout
is adopted. The **Data layout** row in the details shows the result ("As documented" or
"Shifted by 4 bits").

On that device, `getMessageId()` was the subframe number, or -1 for about a third of the pages,
and `getSubmessageId()` held the word type, which matches the documentation for
`submessage_id` ("the word type"). The Pixel 10 Pro XL and Pixel 11 Pro also put the word type
there.

### Page halves paired the wrong way

A nominal page is an even page part followed by an odd page part. If a device reports the odd
part of one page together with the even part of the next, GPSTest rejoins the halves of two
consecutive messages from the same satellite, and keeps the result only if the CRC passes. The
**rejoined** count in the page counters shows how often this happened.

### Pages delivered twice

The Pixel 11 Pro delivers many Galileo pages twice in a row, with the same bits: always the
pages in slots 0, 3, 6, 9 and 12 of a subframe (every third page), and at times the others too
(54 % of the pages in the test log). A page with the same bits as the satellite's previous one,
less than 2 s (one page) after it, is dropped. A new page can't arrive that soon and always
differs in its time, word type or OSNMA bits. The **repeated** count in the page counters shows
how many were dropped. Reported to Google as [issue 583449761](https://issuetracker.google.com/issues/583449761).

### Missing pages

Phones often deliver only part of the pages: the chip saves power (duty cycling) or tracks the E1
pilot signal, which carries no data. The Pixel 8 Pro log contained only 6 to 29 % of each Galileo
satellite's pages, even with full tracking requested (reported to Google as [issue
580885286](https://issuetracker.google.com/issues/580885286)). With complete subframes only,
authentication couldn't complete at such rates; with the page-by-page processing above it can, but
it takes longer, mostly until all parts of the TESLA root key have been received once. The
**Subframes** row shows how many subframes were complete, and the card shows a hint when fewer than
half are. Turning on **Force full GNSS measurements**, both in GPSTest's settings and in Android's
developer options, and keeping the screen on can help.

### Time of each page

Android doesn't say when a page was transmitted. GPSTest takes the Galileo time from the words
that carry it (word types 0, 5 and 6) and places other pages in the slot their word type occupies
in the fixed E1-B subframe sequence. This tolerates delivery delays of several seconds. Pages
that fail the CRC are never used for timing. The **Page timing** and **Clock offset** rows show
how this is going.

On devices that provide raw GNSS measurements, the receiver's own clock (`GnssClock`, Android 10
and later) gives the GNSS time at a known moment. GPSTest then estimates each page's time from
its arrival, without waiting for the first time stamp in the data, and only uses the word type
to settle on the exact slot. **Page timing** then says "From the receiver clock". Otherwise
it says why the receiver clock isn't used, for example "the chip doesn't report the GNSS time"
(no `FullBiasNanos`) or "the chip's GNSS time is still uncertain" (more than 1 ms). The Galileo
time read from the signal is still compared with the reference time (see [Time check](#time-check)).

`getMessageId()` should be the subframe number (1 to 24) of a Galileo I/NAV page. GPSTest only
uses it once it agrees with the time stamps in the data, which settles pages whose word type
fits several slots. The Pixel 11 Pro numbers each page by the subframe it *ends* in, so the
last page of a subframe (slot 14) carries the next subframe's number; GPSTest also considers
the previous subframe for that slot (reported to Google as [issue 583431466](https://issuetracker.google.com/issues/583431466)). The Pixel
10 Pro XL reports numbers from 1 to 30 that don't match the subframes, so they're not used there
(reported as [issue 583449762](https://issuetracker.google.com/issues/583449762)).

## Logging

GPSTest can write the OSNMA result to its CSV log file (off by default). The format is described
in [LOGGING.md](LOGGING.md).

# Play Console questionnaire answers

Use these prepared answers for FP-30X Companion 0.4.8. Exact labels can change in
Play Console; answer the current form consistently with the statements below.

## Store settings

| Field | Prepared answer |
|---|---|
| App or game | App |
| Category | Music & Audio |
| Tags | Music player/controller, MIDI, Musical instrument where available |
| Free or paid | Free |
| Contains ads | No |
| In-app purchases | No |
| Default language | English (United States) |
| Additional language | Russian |
| Support email | idlelink@yandex.ru |

## App access

- No login, membership, or authentication is required.
- The interface, tone catalog, settings, and local preset workflow can be reviewed
  without special access.
- Connection, MIDI transmission, and recording require a Roland FP-30X or compatible
  MIDI/USB device. This is a hardware dependency, not an access restriction.

Reviewer instructions:

```text
No account or credentials are required. Open Piano and tap the displayed tone to
browse the 321-tone catalog. Presets and settings can be inspected without a piano.
Bluetooth MIDI, USB MIDI, and recording functions require compatible external
hardware. The app has no internet-backed or remotely restricted functionality.
```

## Ads and monetization

- Ads: **No**.
- In-app products/subscriptions: **No**.
- Financial features: **None**.

## Target audience and content

- Intended age group: **13 and older**.
- Not designed primarily for children.
- No child-directed advertising or data collection.
- No user-generated content, social features, messaging, web browsing, gambling,
  purchases, violence, sexual content, controlled substances, or profanity.
- Expected IARC result: suitable for all ages, subject to the current questionnaire.

## News, health, government, and regulated features

- News or magazine app: **No**.
- Health app or health features: **No**.
- Government app: **No**.
- Financial app: **No**.
- VPN service: **No**.
- Accessibility service API: **Not used**.
- Exact alarm permission: **Not used**.
- Full-screen intent permission: **Not used**.
- Photos and videos permission: **Not used**.

## Account deletion

The app does not let users create accounts and keeps no server-side user data, so
the account-deletion requirement is not applicable. Users delete local private data
through Android **Settings > Apps > FP-30X Companion > Storage > Clear data** or by
uninstalling. Recordings saved in shared storage must be deleted separately.

## Foreground service declaration

The app declares a microphone foreground service solely for a recording explicitly
started by the user. While it runs, Android displays a persistent notification named
**FP-30X recordings** with a Stop action. The service ends when the user stops the
recording or when recording fails.

Recommended declaration wording:

```text
Core functionality: user-initiated recording of the digital piano's USB audio and
incoming MIDI performance. A microphone-type foreground service keeps recording
active when the app leaves the foreground and provides a persistent notification
with an immediate Stop action. No audio is uploaded or shared.
```

If Play Console requests a demonstration video for foreground service access, record
a short physical-device flow: connect FP-30X over USB, open Record, select Piano
audio, tap Record, show the persistent notification, then tap Stop.

## Copyright and trademark

The listing must retain this disclaimer:

```text
Roland and FP-30X are trademarks of Roland Corporation. FP-30X Companion is an
independent application and is not affiliated with, endorsed by, or sponsored by
Roland Corporation.
```

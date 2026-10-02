# Google Play publication materials

This directory contains the public files used to publish FP-30X Companion on
Google Play. These files are not required to run the application.

## Information for users

- `compliance/privacy-policy.md` is the readable privacy policy.
- `compliance/privacy-policy.html` is the web version of the same policy.
- `compliance/permissions.md` explains why the application requests each Android
  permission.
- `metadata/android/` contains the English and Russian Google Play descriptions
  and release notes.

FP-30X Companion works offline and does not require an account. Questions about
privacy or the store listing can be sent to `idlelink@yandex.ru`.

## Developer notes

The remaining files support Google Play releases:

- `compliance/` contains Play Console declarations and the reviewer test plan.
- `source/` contains editable source artwork.
- `metadata/android/` uses a Fastlane-compatible directory layout.

### Required store images

Before creating a release, add 2 to 8 real PNG screenshots for each language:

```text
metadata/android/en-US/images/phoneScreenshots/
metadata/android/ru-RU/images/phoneScreenshots/
```

Number the files, for example `01-piano.png` and `02-tones.png`, to keep their
display order. Each screenshot must be between 320 and 3840 pixels on each side,
have an aspect ratio no greater than 2:1, and be no larger than 8 MiB.

Also add these files for both `en-US` and `ru-RU`:

```text
images/icon.png
images/featureGraphic.png
```

The icon must be 512 x 512 pixels. The feature graphic must be 1024 x 500 pixels.

### Create a release

Run the release command from the project root:

```bash
./build.py
```

The command validates the store text, images, signed AAB, manifest, and checksum.
The completed publication package is written to `build/outputs/release/`.

Before promoting a release to production:

1. Publish `compliance/privacy-policy.html` at a public HTTPS URL.
2. Upload the AAB to an internal testing track.
3. Complete the Play Console declarations using the files in `compliance/`.
4. Test Bluetooth MIDI, USB MIDI, and USB audio on physical hardware.

### Keep signing credentials private

The upload key and its properties file must remain outside the repository:

```text
$HOME/.android/fp30x-companion-upload.jks
$HOME/.android/fp30x-companion-upload.properties
```

Keep an encrypted backup of both files. With Play App Signing, this is the upload
key; Google manages the separate application-signing key.

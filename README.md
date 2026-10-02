# FP-30X Companion

FP-30X Companion is an offline Android controller and recorder for Roland FP-30X.
It does not require a Roland account, registration, or internet access. The default
interface language is English; Android automatically uses the included Russian
translation when the system language is Russian.

## Features

- all 321 FP-30X tones, alphabetically sorted, searchable, and grouped by category;
- colored Favorites filter;
- automatic Single, Dual, and Split modes from the selected tone roles;
- named presets with Save, dialog-free Duplicate (`[C]` prefix), Load, Delete, and
  direct removal of tone assignments with the minus control;
- automatic restoration of the last active preset after connecting;
- tones and settings selected while disconnected are applied automatically on connection;
- split point, part balance, octave shifts, transpose, key touch, ambience, and
  brilliance controls;
- metronome switch, tempo, beat, and volume controls;
- MIDI recording over Bluetooth MIDI or USB MIDI;
- digital stereo piano-audio recording over a compatible USB connection;
- simultaneous WAV and MIDI recording with a common timestamp;
- direct saving to Downloads or a directory selected through Android's folder picker;
- an About tab with the installed version and a link to the source repository.

## Piano tab

The compact list shows only the currently active tones. Press **＋** to open the
complete tone catalog. For a tone, select one of the available roles:

- **Single** replaces the current setup with one tone;
- **Main** plus **Layer** creates Dual mode;
- **Right** plus **Left** creates Split mode.

The mode shown above the active list is derived automatically; there is no separate
mode switch. Press the **minus** control on an active tone to remove its assignment.

Actions beside the mode name:

- the **diskette** saves the current setup. The dialog contains the current preset
  name, so it can also be renamed;
- the **preset-list** icon opens saved presets. Duplicate is available in a preset’s
  action menu and immediately creates `[C] <old name>`.

Tap the performance summary table to open piano settings. Tap the separate metronome
summary table to open metronome settings.

Performance settings are part of presets. Metronome state, connection type,
recording choices, and recording directory are not part of presets.

## Tone filters

The catalog has one filter dropdown plus text search. Gray uppercase rows are visual
section headings and cannot be selected. **Favorites** remains highlighted in orange.
There are no separate FP-30X and GM2 switches.

- **Quick selection:** All, Favorites.
- **Keyboards:** Acoustic pianos, Electric pianos, Harpsichords & clavinets,
  Mallet keyboards & bells, Organs & accordions.
- **Strings & orchestra:** Guitars, Basses, Solo strings & harps, Ensembles,
  choirs & orchestra, Brass, Saxophones & reeds, Flutes & pipes.
- **Synthesizers:** Synth leads, Synth pads, Atmospheres & textures.
- **World & rhythm:** World instruments, Percussion, Drum kits.
- **Sound effects:** All sound effects, Instrument & performance noises, Nature,
  Animals, Household & signals, Transport & machinery, People & body, Shots &
  explosions.

Filters are based on the sound family, regardless of whether a tone is built into the
FP-30X panel or comes from GM2. When an identical bank/program is present in both,
the catalog shows only the direct built-in version. This removes duplicate entries
such as AcousticBass without removing a distinct sound.

## Bluetooth MIDI connection

### Enable Bluetooth MIDI on the FP-30X

1. Turn on the piano.
2. Hold the **Function** button.
3. While holding it, press **A0**, the leftmost white key.
4. Release both.

Do not pair the piano in Android's ordinary Bluetooth settings. Close Roland Piano
App and other MIDI programs first; then in FP-30X Companion:

1. select **Bluetooth MIDI** in the bottom bar;
2. press **Connect**;
3. allow **Nearby devices**. Android 11 and older may ask for Location because that
   Android version uses it for Bluetooth scanning; this app does not read location;
4. wait for `Connected: FP-30X MIDI`.

The bottom bar is padded above both gesture navigation and three-button navigation.

### GM2 tones and A1

GM2 (General MIDI 2) is the extra bank containing saxophones, guitars, brass,
effects, and many other instruments that cannot be selected from the piano panel.
To let external Bank Select/Program Change commands control the played keyboard,
hold **Function** and press **A1**, the eighth white key from the left.

```text
Left edge
[A0] [B0] [C1] [D1] [E1] [F1] [G1] [A1]
 ↑ Bluetooth on                           ↑ SMF Play Mode: Internal
```

`DT1` is Roland's name for a settings command. The application generates DT1,
Bank Select, and Program Change messages automatically; the user never enters them.

## USB connection

Use the rear **USB Computer** Type-B port, not USB Memory. Connect it with a
USB-B-to-USB-C data cable or a USB-B-to-USB-A cable and a USB-OTG adapter. The
piano still uses its own power adapter.

1. Turn on FP-30X.
2. Connect USB Computer to the phone.
3. Enable OTG in MIUI/HyperOS if the phone has an OTG switch.
4. Select **USB** in the bottom bar and press **Connect**.
5. Select Roland FP-30X if Android reports more than one MIDI device.

USB and Bluetooth can both select tones, configure the piano, and record MIDI.
Bluetooth MIDI does **not** carry the piano's sound. USB additionally exposes the
FP-30X digital audio input on compatible Android devices, so the app can record the
sound generator directly without using the phone microphone.

`FP-30X Audio` shown in the Bluetooth settings is the opposite direction: it plays
phone audio through the piano speakers. It cannot send the played piano sound back
to the phone.

## Record tab

The available checkboxes reflect the current connection:

- **MIDI notes and pedals (MID)** works when the Bluetooth or USB MIDI connection
  exposes incoming MIDI;
- **Piano audio (WAV)** works only when Android routes an FP-30X USB audio input to
  the application. It remains disabled over Bluetooth.

Select either checkbox or both and press **Record**. While recording, the bottom
connection bar becomes a red `● REC` status with elapsed time and **Stop**; accidental
disconnection is unavailable. The foreground notification also has a Stop action.

By default files are saved to Downloads. Press **Change** to select another writable
folder. Filenames are:

```text
fp30x_yyyyMMdd_HHmmss.mid
fp30x_yyyyMMdd_HHmmss.wav
```

When both formats are selected, they use the same timestamp stem. The app has no
recording library, playback screen, upload, or cloud synchronization.

Android labels the USB input permission as **Record audio**. It is required only to
open the selected audio input; the application verifies the actual routed device and
refuses capture when Android routes another microphone.

## Permissions and stored data

- Nearby devices / legacy Location: Bluetooth MIDI discovery only;
- Record audio: direct USB audio capture only;
- folder access: only the directory selected in Android's system picker;
- no INTERNET permission is declared.

Favorites and presets are stored in the application's private data. Normal uninstall
removes that local data; Android's system backup may restore it after reinstall when
backup is enabled. Recordings in Downloads or a selected external folder are not
removed with the application.

## Compatibility

| Device / platform | MIDI build status | USB audio hardware status |
|---|---:|---:|
| Android 6.0–16 (API 23–36) | Built and covered by automated tests | Depends on USB host/audio routing |
| Redmi 14 / HyperOS 3 / Android 16 | Targeted | Requires real-device verification |
| POCO M3 | Targeted | Requires real-device verification |
| Roland FP-30X Bluetooth MIDI | Protocol implemented | Audio is not available over Bluetooth |
| Roland FP-30X USB Computer | MIDI and USB-audio paths implemented | Requires phone/cable verification |

No Android phone or FP-30X is attached to the build host, so the rows marked
"Requires real-device verification" are intentionally not claimed as verified.
When testing USB audio, confirm that the saved WAV contains the piano's clean stereo
signal and not the phone microphone.

## Troubleshooting

If Bluetooth does not connect:

1. close Roland Piano App and other MIDI clients;
2. disconnect laptops and other phones from the piano;
3. enable piano Bluetooth with **Function + A0**;
4. select Bluetooth MIDI, not FP-30X Audio;
5. restart phone Bluetooth and the piano, then retry.

If built-in tones work but GM2 tones do not, press **Function + A1** once and retry.
If USB is missing on Redmi/POCO, enable OTG and use a data-capable cable.
If Audio is disabled but USB MIDI works, Android did not expose/reroute an FP-30X
input; MIDI recording remains available.

## Developer build

Requirements: JDK 17 and Android SDK Platform 36.

### Debug APK

```bash
./build.py --debug
```

The output is `build/outputs/debug/app-debug.apk`.
The debug variant uses application ID `dev.idlesign.fp30xcompanion.debug`, version
suffix `-debug`, and the launcher name `FP-30X Companion Debug`. It can therefore be
installed alongside the release or Google Play application. Android keeps its app
data and settings separate from the release variant.

### Release APK

Build the non-debuggable release variant:

```bash
./gradlew clean test lint assembleRelease
```

Gradle writes the unsigned input to
`build/work/outputs/apk/release/app-release-unsigned.apk`. Android will not install
this file until it is signed. Create a private signing key once; keep it
outside the repository and reuse the same key for every update:

```bash
mkdir -p "$HOME/.android"
keytool -genkeypair -v \
  -keystore "$HOME/.android/fp30x-release.jks" \
  -alias fp30x-companion \
  -keyalg RSA -keysize 2048 -validity 10000
```

Align and sign each release build. Set `ANDROID_HOME` to your Android SDK directory
first if it is not already defined:

```bash
BUILD_TOOLS="$(find "$ANDROID_HOME/build-tools" \
  -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
mkdir -p build/outputs/release
"$BUILD_TOOLS/zipalign" -f -p 4 \
  build/work/outputs/apk/release/app-release-unsigned.apk \
  build/outputs/release/FP30X-Companion-release-aligned.apk
"$BUILD_TOOLS/apksigner" sign \
  --ks "$HOME/.android/fp30x-release.jks" \
  --ks-key-alias fp30x-companion \
  --out build/outputs/release/FP30X-Companion-release.apk \
  build/outputs/release/FP30X-Companion-release-aligned.apk
"$BUILD_TOOLS/apksigner" verify --verbose \
  build/outputs/release/FP30X-Companion-release.apk
```

`build/outputs/release/FP30X-Companion-release.apk` is the installable
release file. Do not delete
or replace the keystore: Android accepts an update over an installed copy only when
both APKs are signed with the same key.

## Google Play release package

Build, test, lint, sign, and package the Google Play AAB with one command:

```bash
./build.py
```

For a debug APK that increments the patch component and `versionCode`:

```bash
./build.py --debug
```

A successful debug build increments the SemVer patch component (for example,
`0.4.8` becomes `0.4.9`) and `versionCode`. A release build automatically increments
the SemVer minor component (for example,
`0.4.8` becomes `0.5.0`), resets the patch component to zero, and increments
`versionCode`. If the release fails, both version fields are restored. The debug
APK is published at `build/outputs/debug/app-debug.apk` and installs as the
separate `dev.idlesign.fp30xcompanion.debug` application. The script
reads the upload key from `$HOME/.android/fp30x-companion-upload.properties` by
default; override the path with `FP30X_UPLOAD_KEY_PROPERTIES`. Final artifacts are
grouped by build type: the APK is written to `build/outputs/debug/`, while the signed
AAB, submission ZIP, checksums, manifest, and store materials are written to
`build/outputs/release/`. Real screenshots must be placed under
`pub/metadata/android/<locale>/images/phoneScreenshots/`; `pub/README.md` documents
all required store image paths and dimensions. The build validates and copies these
files unchanged instead of generating mock screenshots.

## Reference documents

- [Roland FP-30X owner's manual](https://static.roland.com/assets/media/pdf/FP-30X_eng04_W.pdf)
- [Roland: Bluetooth MIDI connection](https://support.roland.com/hc/en-us/articles/14974096737179-FP-30X-How-to-connect-to-a-Mac-via-Bluetooth-MIDI)
- [Roland: externally selected tones](https://support.roland.com/hc/en-us/articles/24404854810395-FP-30X-How-can-I-manually-play-the-tone-specified-from-a-DAW)

## License

FP-30X Companion is licensed under the BSD 3-Clause License. See
[`LICENSE`](LICENSE) for details.

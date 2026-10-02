# Reviewer and release test plan

## Review without hardware

1. Launch the app; no sign-in or network access is required.
2. On **Piano**, tap the current tone and verify the searchable tone catalog.
3. Assign tones to Single, Main/Layer, or Right/Left roles.
4. Save, duplicate, load, and delete a local preset.
5. Open the piano and metronome settings summaries.
6. Open **Record** and verify that unavailable inputs remain disabled while
   disconnected.

## Required publisher hardware test

1. Install the Play-generated APK from an internal testing track on Android 12 or
   later and on one Android 11-or-earlier device if available.
2. Connect a Roland FP-30X through Bluetooth MIDI. Grant Nearby devices, choose a
   tone, and verify the piano changes sound.
3. Connect through the rear USB Computer port with USB-OTG. Verify tone changes and
   incoming MIDI recording.
4. If Android exposes FP-30X audio input, record WAV and MIDI simultaneously for at
   least 30 seconds. Confirm both files share a timestamp stem and that the WAV
   contains the piano's stereo digital signal, not the handset microphone.
5. Background the app during recording. Verify the ongoing notification, elapsed
   time, Stop action, and finalized files.
6. Deny each runtime permission once and confirm the app explains the missing
   permission without crashing.
7. Revoke a selected folder's access and verify the app falls back to Downloads.
8. Confirm the app functions in airplane mode and produces no network traffic.

## Production gate

- All unit, lint, and release bundle tasks pass.
- Bundle is accepted by Play Console with package `dev.idlesign.fp30xcompanion`,
  `versionCode` 14, and target SDK 36.
- Store text and screenshots match the tested build.
- Privacy-policy HTTPS URL is live and entered in Play Console.
- Upload keystore and credentials have an encrypted backup.

# Privacy Policy for FP-30X Companion

Effective date: October 1, 2026

FP-30X Companion is an independent Android application for controlling and
recording a Roland FP-30X digital piano. This policy explains how the application
handles information.

## Data collection and sharing

FP-30X Companion does not collect, transmit, sell, or share personal data. The
application has no internet permission, contains no advertising or analytics SDK,
and does not provide accounts or cloud synchronization.

## Data stored on the device

The application stores favorites, presets, the last active preset, and app settings
in its private storage. Android may include this private data in the user's encrypted
system backup when device backup is enabled.

MIDI and WAV recordings are created only when the user starts recording. They are
saved to Downloads or to a folder the user selects through Android's system folder
picker. The application does not upload these files. The user can delete recordings
with any file manager. Uninstalling the application removes its private data but
does not remove recordings saved in shared storage.

## Permissions

- **Nearby devices / Bluetooth:** used only to discover and connect to a compatible
  Bluetooth MIDI device. On Android 11 and earlier, Android requires the Location
  permission for Bluetooth discovery; the application does not obtain or store the
  user's location.
- **Record audio:** used only when the user chooses to record the FP-30X digital
  audio input over USB. Audio is written locally to a WAV file and is never
  transmitted. The application checks that Android routed a supported USB audio
  device rather than the phone microphone.
- **Files and folders:** used only to create recordings in Downloads or in a folder
  selected by the user through Android's system picker.
- **Foreground service:** keeps a user-initiated recording active while the
  application is not in the foreground and displays an ongoing notification with a
  Stop action.

## Data retention and deletion

No data is kept on a developer-operated server. Users can clear private app data in
Android settings or uninstall the application. Recordings in shared storage remain
under the user's control and can be deleted with a file manager.

## Children

The application is a utility for digital-piano owners and is not directed to
children. It does not knowingly collect personal information from children or from
any other user.

## Changes to this policy

If application behavior changes, this policy will be updated before a release that
introduces new data handling. The effective date above identifies the current
version.

## Contact

Questions about this policy can be sent to idlelink@yandex.ru.

Roland and FP-30X are trademarks of Roland Corporation. FP-30X Companion is an
independent application and is not affiliated with, endorsed by, or sponsored by
Roland Corporation.

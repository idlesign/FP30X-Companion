# Permission justification

| Permission | Platform scope | User-facing purpose |
|---|---|---|
| `BLUETOOTH_SCAN` | Android 12+ | Find FP-30X Bluetooth MIDI |
| `BLUETOOTH_CONNECT` | Android 12+ | Open and use the selected Bluetooth MIDI device |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | Android 11 and older | Legacy Bluetooth MIDI discovery and connection |
| `ACCESS_FINE_LOCATION` | Android 11 and older only | Required by Android for BLE discovery; no location is read or stored |
| `RECORD_AUDIO` | All supported versions | Capture the selected FP-30X USB audio input after the user starts recording |
| `FOREGROUND_SERVICE` | Android 9+ | Keep an active user-initiated recording running with a visible notification |
| `FOREGROUND_SERVICE_MICROPHONE` | Android 14+ | Declare the microphone-type recording foreground service |
| `WRITE_EXTERNAL_STORAGE` | Android 9 and older only | Save user-requested recording files on legacy Android storage |

The app deliberately does not declare `INTERNET`, broad media-library permissions,
background location, or access to contacts, identity, advertising ID, camera, or
phone state.

# Google Play Data safety answers

These answers describe version 0.4.8 (`versionCode` 14). Re-evaluate them whenever
the app adds an SDK, network permission, telemetry, account, or cloud feature.

## Data collection and sharing

| Play Console question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | No |
| Is all user data collected by your app encrypted in transit? | Not applicable; no data leaves the device |
| Do you provide a way for users to request that their data is deleted? | Not applicable; no server-side data or account exists |

Do not select any data-type checkboxes. The following information is processed only
on the device and is not collected under Google Play's off-device definition:

- Bluetooth device identifiers used during an active discovery/connection session;
- MIDI notes and pedal events used to create a local `.mid` file;
- USB audio samples used to create a local `.wav` file;
- favorites, presets, connection choice, and recording destination stored locally.

Android system backup may transfer private app preferences under the user's Android
backup settings. This is an operating-system service rather than transmission to a
developer-operated service. Confirm this interpretation against the wording shown
in Play Console at submission time.

## Supporting evidence

- `app/src/main/AndroidManifest.xml` declares no `android.permission.INTERNET`.
- The app has no analytics, advertising, authentication, or network SDK dependency.
- Recordings are written to Downloads or a user-selected Storage Access Framework
  directory.

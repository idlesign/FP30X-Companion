package dev.idlesign.fp30xcompanion;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.midi.MidiDeviceInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity implements
        MidiConnectionManager.Listener,
        PerformanceController.Listener {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 10;
    private static final int REQUEST_ENABLE_BLUETOOTH = 11;
    private static final int REQUEST_RECORD_AUDIO = 12;
    private static final int REQUEST_RECORDING_DIRECTORY = 13;
    private static final int COLOR_INK = Color.rgb(24, 28, 35);
    private static final String STATE_TONE_FAMILY = "tone_family";
    private static final String STATE_TONE_SEARCH = "tone_search";

    private final ArrayList<Tone> allTones = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>();
    private MidiConnectionManager connection;
    private PerformanceController performanceController;
    private SharedPreferences preferences;
    private ConnectionBar connectionBar;
    private PianoView pianoView;
    private RecordView recordView;
    private InfoView infoView;
    private RecordingController recordingController;
    private RecordingDestination recordingDestination;
    private boolean pendingBluetoothConnect;
    private boolean pendingRecordAudio;
    private boolean pendingRecordMidi;
    private boolean connected;
    private boolean midiInputAvailable;
    private ConnectionTarget connectedTarget = ConnectionTarget.BLUETOOTH_MIDI;
    private ToneFamily selectedToneFamily = ToneFamily.ALL;
    private String toneSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoreToneFilter(savedInstanceState);
        preferences = getSharedPreferences("fp30x", MODE_PRIVATE);
        favorites.addAll(preferences.getStringSet("favorites", Collections.emptySet()));
        connection = new MidiConnectionManager(this, this);
        recordingDestination = RecordingDestination.load(this);
        recordingController = new RecordingController(this, connection, new RecordingController.Listener() {
            @Override
            public void onRecordingStarted(String summary) {
                recordView.setRecording(true);
                connectionBar.showRecording("● REC  00:00.0  " + summary, recordingController::stop);
            }

            @Override
            public void onRecordingProgress(long elapsedMillis, float level) {
                recordView.setProgress(elapsedMillis, level);
                connectionBar.showRecording("● REC  " + formatElapsed(elapsedMillis), recordingController::stop);
            }

            @Override
            public void onRecordingStopped(List<String> files) {
                recordView.setRecording(false);
                refreshRecordingCapabilities();
                if (connected) {
                    connectionBar.hideRecording(getString(R.string.status_connected, "FP-30X"));
                } else {
                    connectionBar.hideRecording(getString(R.string.status_not_connected));
                    connectionBar.showDisconnected(getString(R.string.status_not_connected));
                }
                String directory = recordingDestination.kind == RecordingDestination.Kind.DOWNLOADS
                        ? getString(R.string.recording_destination_downloads)
                        : recordingDestination.treeUri.getLastPathSegment();
                Toast.makeText(MainActivity.this,
                        getString(R.string.recording_saved_to,
                                directory, String.join(", ", files)),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onRecordingError(String message) {
                if (recordView != null) {
                    recordView.setRecording(false);
                }
                showError(message);
            }
        });
        try {
            allTones.addAll(ToneCatalog.load(this));
            Collections.sort(allTones,
                    (left, right) -> left.name.compareToIgnoreCase(right.name));
        } catch (IOException error) {
            Toast.makeText(this, getString(R.string.error_catalog, error.getMessage()),
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        PresetRepository repository = new PresetRepository(this, allTones);
        performanceController = new PerformanceController(repository, connection, this);
        setContentView(buildUi());
        pianoView.render(
                performanceController.state(),
                performanceController.activePreset(),
                performanceController.isDirty(),
                performanceController.metronomeState());
        refreshRecordingCapabilities();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 247, 249));

        FrameLayout content = new FrameLayout(this);
        pianoView = new PianoView(this, new PianoView.Listener() {
            @Override
            public void onChooseTone(ToneRole role) {
                showToneCatalog(role);
            }

            @Override
            public void onSave() {
                PresetSaveDialog.show(
                        MainActivity.this,
                        performanceController.activePreset().name,
                        performanceController::save);
            }

            @Override
            public void onPresets() {
                showPresets();
            }

            @Override
            public void onSettings() {
                PianoSettingsDialog.show(
                        MainActivity.this,
                        performanceController.state(),
                        new PianoSettingsDialog.Listener() {
                            @Override
                            public void onPerformancePreview(PerformanceState state) {
                                performanceController.previewPerformance(state);
                            }

                            @Override
                            public void onApply(PerformanceState state, boolean changed) {
                                if (changed) {
                                    performanceController.updateSettings(state);
                                }
                            }

                            @Override
                            public void onCancel() {
                                performanceController.cancelSettingsPreview();
                            }
                        });
            }

            @Override
            public void onMetronome() {
                MetronomeDialog.show(
                        MainActivity.this,
                        performanceController.metronomeState(),
                        new MetronomeDialog.Listener() {
                            @Override
                            public void onPreview(MetronomeState state) {
                                performanceController.previewMetronome(state);
                            }

                            @Override
                            public void onApply(MetronomeState state, boolean changed) {
                                if (changed) {
                                    performanceController.updateMetronome(state);
                                }
                            }

                            @Override
                            public void onCancel() {
                                performanceController.cancelSettingsPreview();
                            }
                        });
            }

            @Override
            public void onRemove(ToneRole role) {
                performanceController.remove(role);
            }
        });
        recordView = new RecordView(this, new RecordView.Listener() {
            @Override
            public void onStart(boolean audio, boolean midi) {
                startRecording(audio, midi);
            }

            @Override
            public void onStop() {
                recordingController.stop();
            }

            @Override
            public void onChangeDestination() {
                chooseRecordingDirectory();
            }
        });
        recordView.setDestination(recordingDestination);
        infoView = new InfoView(this);
        content.addView(pianoView, matchFrame());
        content.addView(recordView, matchFrame());
        content.addView(infoView, matchFrame());
        recordView.setVisibility(View.GONE);
        infoView.setVisibility(View.GONE);

        TopTabBar tabs = new TopTabBar(this, index -> {
            pianoView.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
            recordView.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
            infoView.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        });
        root.addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        connectionBar = new ConnectionBar(this, new ConnectionBar.Listener() {
            @Override
            public void onConnect(ConnectionTarget target) {
                connectedTarget = target;
                if (target == ConnectionTarget.USB) {
                    chooseUsbDevice();
                } else {
                    startBluetoothConnection();
                }
            }

            @Override
            public void onDisconnect() {
                connection.disconnect();
            }
        });
        root.addView(connectionBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private FrameLayout.LayoutParams matchFrame() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void startRecording(boolean audio, boolean midi) {
        if (!audio && !midi) {
            showError(getString(R.string.error_select_recording_source));
            return;
        }
        if (audio && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingRecordAudio = audio;
            pendingRecordMidi = midi;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        recordingController.start(audio, midi, recordingDestination);
    }

    private void chooseRecordingDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_RECORDING_DIRECTORY);
    }

    private void refreshRecordingCapabilities() {
        if (recordView == null) {
            return;
        }
        boolean audioDetected = recordingController.audioCapabilities() != null;
        RecordingPresentation presentation = RecordingPresentation.from(
                connected,
                connectedTarget,
                midiInputAvailable,
                audioDetected,
                recordingController.isRecording());
        recordView.setCapabilities(
                connected,
                presentation.midiEnabled,
                presentation.audioEnabled);
    }

    private String formatElapsed(long elapsedMillis) {
        long minutes = elapsedMillis / 60_000;
        double seconds = (elapsedMillis % 60_000) / 1000.0;
        return String.format(java.util.Locale.ROOT, "%02d:%04.1f", minutes, seconds);
    }

    private void showToneCatalog(ToneRole replacementRole) {
        ToneCatalogDialog dialog = new ToneCatalogDialog(
                this,
                allTones,
                favorites,
                replacementRole,
                selectedToneFamily,
                toneSearchQuery,
                new ToneCatalogDialog.Listener() {
                    @Override
                    public PerformanceState currentState() {
                        return performanceController.state();
                    }

                    @Override
                    public void onFilterSelected(ToneFamily family) {
                        selectedToneFamily = family;
                    }

                    @Override
                    public void onSearchChanged(String query) {
                        toneSearchQuery = query;
                    }

                    @Override
                    public void onAssignSingle(Tone tone) {
                        performanceController.assignSingle(tone);
                    }

                    @Override
                    public void onAssignPrimary(Tone tone) {
                        performanceController.assignPrimary(tone);
                    }

                    @Override
                    public void onAssignLayer(Tone tone) {
                        performanceController.assignLayer(tone);
                    }

                    @Override
                    public void onAssignLeft(Tone tone) {
                        performanceController.assignLeft(tone);
                    }

                    @Override
                    public void onToggleFavorite(Tone tone) {
                        toggleFavorite(tone);
                    }
                });
        dialog.show();
    }

    private void restoreToneFilter(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String familyName = savedInstanceState.getString(STATE_TONE_FAMILY);
        if (familyName != null) {
            try {
                selectedToneFamily = ToneFamily.valueOf(familyName);
            } catch (IllegalArgumentException ignored) {
                selectedToneFamily = ToneFamily.ALL;
            }
        }
        toneSearchQuery = savedInstanceState.getString(STATE_TONE_SEARCH, "");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_TONE_FAMILY, selectedToneFamily.name());
        outState.putString(STATE_TONE_SEARCH, toneSearchQuery);
        super.onSaveInstanceState(outState);
    }

    private void showPresets() {
        new PresetListDialog(this, new PresetListDialog.Listener() {
            @Override
            public List<Preset> presets() {
                return performanceController.presets();
            }

            @Override
            public void onLoad(String id) {
                performanceController.load(id);
            }

            @Override
            public void onDuplicate(String id) {
                performanceController.duplicate(id);
            }

            @Override
            public void onDelete(String id) {
                performanceController.delete(id);
            }
        }).show();
    }

    private void toggleFavorite(Tone tone) {
        String id = tone.stableId();
        if (!favorites.add(id)) {
            favorites.remove(id);
        }
        preferences.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
    }

    private void startBluetoothConnection() {
        if (!hasBluetoothPermissions()) {
            pendingBluetoothConnect = true;
            requestBluetoothPermissions();
            return;
        }
        if (!connection.isBluetoothAvailable()) {
            showError(getString(R.string.error_bluetooth_le_unavailable));
            return;
        }
        if (!connection.isBluetoothEnabled()) {
            pendingBluetoothConnect = true;
            try {
                startActivityForResult(
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        REQUEST_ENABLE_BLUETOOTH);
            } catch (SecurityException error) {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            }
            return;
        }
        pendingBluetoothConnect = false;
        connection.scanAndConnectBluetooth();
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                startRecording(pendingRecordAudio, pendingRecordMidi);
            } else {
                showError(getString(R.string.error_audio_permission));
            }
            pendingRecordAudio = false;
            pendingRecordMidi = false;
            return;
        }
        if (requestCode != REQUEST_BLUETOOTH_PERMISSIONS) {
            return;
        }
        if (hasBluetoothPermissions() && pendingBluetoothConnect) {
            startBluetoothConnection();
        } else {
            pendingBluetoothConnect = false;
            showError(getString(R.string.error_nearby_permission));
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORDING_DIRECTORY && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            int grantFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(data.getData(), grantFlags);
                recordingDestination = RecordingDestination.tree(data.getData());
                recordingDestination.persist(this);
                recordView.setDestination(recordingDestination);
            } catch (SecurityException error) {
                showError(getString(R.string.error_recording_directory_permission));
            }
            return;
        }
        if (requestCode == REQUEST_ENABLE_BLUETOOTH && pendingBluetoothConnect) {
            if (resultCode == RESULT_OK) {
                startBluetoothConnection();
            } else {
                pendingBluetoothConnect = false;
                showError(getString(R.string.error_bluetooth_not_enabled));
            }
        }
    }

    private void chooseUsbDevice() {
        List<MidiDeviceInfo> devices = connection.usbDevices();
        if (devices.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.usb_not_found_title)
                    .setMessage(R.string.usb_not_found_message)
                    .setPositiveButton(R.string.button_ok, null)
                    .show();
            return;
        }
        if (devices.size() == 1) {
            connection.connectUsb(devices.get(0));
            return;
        }
        String[] names = new String[devices.size()];
        for (int index = 0; index < devices.size(); index++) {
            names[index] = connection.deviceName(devices.get(index));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.usb_choose_title)
                .setItems(names, (dialog, which) -> connection.connectUsb(devices.get(which)))
                .show();
    }

    @Override
    public void onStatus(String status) {
        if (connectionBar != null) {
            connectionBar.showConnecting(status);
        }
    }

    @Override
    public void onConnected(String deviceName) {
        connected = true;
        refreshRecordingCapabilities();
        if (connectionBar != null) {
            connectionBar.showConnected(getString(R.string.status_connected, deviceName));
        }
    }

    @Override
    public void onReady() {
        performanceController.connectionReady();
    }

    @Override
    public void onMidiInputCapabilities(MidiInputCapabilities capabilities) {
        midiInputAvailable = capabilities.available;
        refreshRecordingCapabilities();
    }

    @Override
    public void onDisconnected() {
        connected = false;
        midiInputAvailable = false;
        if (recordingController != null && recordingController.isRecording()) {
            recordingController.stop();
        }
        refreshRecordingCapabilities();
        if (connectionBar != null) {
            connectionBar.showDisconnected(getString(R.string.status_not_connected));
        }
    }

    @Override
    public void onError(String message) {
        showError(message);
    }

    @Override
    public void onPerformanceChanged(PerformanceState state, Preset activePreset, boolean dirty) {
        if (pianoView != null) {
            pianoView.render(
                    state,
                    activePreset,
                    dirty,
                    performanceController.metronomeState());
        }
    }

    @Override
    public void onPerformanceMessage(String message) {
        if (connectionBar != null && connection.isConnected()) {
            connectionBar.showConnected(message);
        }
    }

    @Override
    public void onPerformanceError(String message) {
        showError(message);
    }

    private void showError(String message) {
        if (connectionBar != null) {
            connectionBar.showError(message);
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        connection.disconnect();
        super.onDestroy();
    }
}

package dev.idlesign.fp30xcompanion;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class MidiConnectionManager implements PerformanceOutput {
    public interface Listener {
        void onStatus(String status);

        void onConnected(String deviceName);

        default void onReady() {
        }

        default void onMidiInputCapabilities(MidiInputCapabilities capabilities) {
        }

        void onDisconnected();

        void onError(String message);
    }

    private static final UUID BLE_MIDI_SERVICE =
            UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");
    private static final long SCAN_TIMEOUT_MILLIS = 12_000;

    private final Context context;
    private final MidiManager midiManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;

    private BluetoothLeScanner scanner;
    private MidiDevice midiDevice;
    private MidiSender sender;
    private MidiOutputPort outputPort;
    private MidiReceiver incomingReceiver;
    private boolean scanning;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = bluetoothName(device);
            if (name == null || name.trim().isEmpty()) {
                name = context.getString(R.string.fallback_bluetooth_midi);
            }
            stopBluetoothScan();
            listener.onStatus(context.getString(R.string.status_connecting, name));
            openBluetoothDevice(device, name);
        }

        @Override
        public void onScanFailed(int errorCode) {
            scanning = false;
            listener.onError(context.getString(R.string.error_bluetooth_scan, errorCode));
        }
    };

    public MidiConnectionManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        try {
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        } catch (SecurityException ignored) {
            return false;
        }
    }

    public void scanAndConnectBluetooth() {
        if (midiManager == null) {
            listener.onError(context.getString(R.string.error_android_midi_unavailable));
            return;
        }
        if (bluetoothAdapter == null) {
            listener.onError(context.getString(R.string.error_bluetooth_le_unavailable));
            return;
        }
        try {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                listener.onError(context.getString(R.string.error_enable_bluetooth));
                return;
            }
            disconnect();
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(BLE_MIDI_SERVICE))
                    .build();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            scanning = true;
            listener.onStatus(context.getString(R.string.status_searching));
            scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
            mainHandler.postDelayed(() -> {
                if (scanning) {
                    stopBluetoothScan();
                    listener.onError(context.getString(R.string.error_piano_not_found));
                }
            }, SCAN_TIMEOUT_MILLIS);
        } catch (SecurityException error) {
            listener.onError(context.getString(R.string.error_bluetooth_permission));
        }
    }

    public List<MidiDeviceInfo> usbDevices() {
        if (midiManager == null) {
            return Collections.emptyList();
        }
        ArrayList<MidiDeviceInfo> result = new ArrayList<>();
        for (MidiDeviceInfo info : midiManager.getDevices()) {
            if (info.getType() == MidiDeviceInfo.TYPE_USB && info.getInputPortCount() > 0) {
                result.add(info);
            }
        }
        return result;
    }

    public String deviceName(MidiDeviceInfo info) {
        String product = info.getProperties().getString(MidiDeviceInfo.PROPERTY_PRODUCT);
        String name = info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
        String manufacturer = info.getProperties().getString(MidiDeviceInfo.PROPERTY_MANUFACTURER);
        if (product != null && !product.trim().isEmpty()) {
            return product;
        }
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (manufacturer != null && !manufacturer.trim().isEmpty()) {
            return manufacturer + " MIDI";
        }
        return context.getString(R.string.fallback_usb_midi);
    }

    public void connectUsb(MidiDeviceInfo info) {
        disconnect();
        String name = deviceName(info);
        listener.onStatus(context.getString(R.string.status_connecting, name));
        midiManager.openDevice(info, device -> finishOpen(device, name), mainHandler);
    }

    public boolean isConnected() {
        return sender != null;
    }

    public void setIncomingReceiver(MidiReceiver receiver) {
        if (outputPort != null && incomingReceiver != null) {
            outputPort.disconnect(incomingReceiver);
        }
        incomingReceiver = receiver;
        if (outputPort != null && receiver != null) {
            outputPort.connect(receiver);
        }
    }

    public boolean canReceiveMidi() {
        return outputPort != null;
    }

    public void applyPerformanceState(PerformanceState state) {
        MidiSender current = sender;
        if (current == null) {
            listener.onError(context.getString(R.string.error_connect_first));
            return;
        }
        current.applyPerformanceState(state);
    }

    @Override
    public void applyMetronomeState(MetronomeState state) {
        MidiSender current = sender;
        if (current == null) {
            listener.onError(context.getString(R.string.error_connect_first));
            return;
        }
        current.applyMetronomeState(state);
    }

    public void selectTone(Tone tone) {
        MidiSender current = sender;
        if (current == null) {
            listener.onError(context.getString(R.string.error_connect_first));
            return;
        }
        current.selectTone(tone);
    }

    public void disconnect() {
        stopBluetoothScan();
        if (sender != null) {
            try {
                sender.close();
            } catch (IOException ignored) {
            }
            sender = null;
        }
        if (outputPort != null) {
            if (incomingReceiver != null) {
                outputPort.disconnect(incomingReceiver);
            }
            try {
                outputPort.close();
            } catch (IOException ignored) {
            }
            outputPort = null;
        }
        if (midiDevice != null) {
            try {
                midiDevice.close();
            } catch (IOException ignored) {
            }
            midiDevice = null;
        }
        listener.onDisconnected();
    }

    private void openBluetoothDevice(BluetoothDevice device, String name) {
        try {
            midiManager.openBluetoothDevice(device, opened -> finishOpen(opened, name), mainHandler);
        } catch (SecurityException error) {
            listener.onError(context.getString(R.string.error_bluetooth_connect_permission));
        }
    }

    private void finishOpen(MidiDevice device, String name) {
        if (device == null) {
            listener.onError(context.getString(R.string.error_midi_open));
            return;
        }
        MidiDeviceInfo info = device.getInfo();
        if (info.getInputPortCount() == 0) {
            try {
                device.close();
            } catch (IOException ignored) {
            }
            listener.onError(context.getString(R.string.error_no_midi_input));
            return;
        }
        MidiInputPort port = device.openInputPort(0);
        if (port == null) {
            try {
                device.close();
            } catch (IOException ignored) {
            }
            listener.onError(context.getString(R.string.error_open_midi_input));
            return;
        }
        midiDevice = device;
        if (info.getOutputPortCount() > 0) {
            outputPort = device.openOutputPort(0);
            if (outputPort != null && incomingReceiver != null) {
                outputPort.connect(incomingReceiver);
            }
        }
        listener.onMidiInputCapabilities(new MidiInputCapabilities(
                outputPort != null, info.getOutputPortCount()));
        sender = new MidiSender(port, error -> mainHandler.post(() -> {
            listener.onError(context.getString(R.string.error_midi_send, error.getMessage()));
            disconnect();
        }));
        sender.sendHandshake();
        MidiSender openedSender = sender;
        listener.onConnected(name);
        mainHandler.postDelayed(() -> {
            if (sender == openedSender) {
                listener.onReady();
            }
        }, 600);
    }

    private void stopBluetoothScan() {
        if (!scanning) {
            return;
        }
        scanning = false;
        try {
            if (scanner != null && hasBluetoothScanPermission()) {
                scanner.stopScan(scanCallback);
            }
        } catch (SecurityException ignored) {
        }
    }

    private boolean hasBluetoothScanPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String bluetoothName(BluetoothDevice device) {
        try {
            return device.getName();
        } catch (SecurityException ignored) {
            return null;
        }
    }
}

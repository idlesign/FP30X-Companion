package dev.idlesign.fp30xcompanion;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecordingSession {
    private final Context context;
    private final MidiConnectionManager connection;
    private final RecordingFileStore fileStore;
    private final RecordingDestination destination;
    private final boolean recordAudio;
    private final boolean recordMidi;
    private final String stem;
    private final MidiRecorder midiRecorder;
    private final UsbAudioRecorder audioRecorder;
    private final File audioTemporary;
    private final ArrayList<String> savedNames = new ArrayList<>();
    private boolean started;

    public RecordingSession(
            Context context,
            MidiConnectionManager connection,
            RecordingFileStore fileStore,
            RecordingDestination destination,
            boolean recordAudio,
            boolean recordMidi,
            String stem,
            UsbAudioCapabilities audioCapabilities,
            UsbAudioRecorder.Listener audioListener
    ) throws IOException {
        this.context = context.getApplicationContext();
        this.connection = connection;
        this.fileStore = fileStore;
        this.destination = destination;
        this.recordAudio = recordAudio;
        this.recordMidi = recordMidi;
        this.stem = stem;
        midiRecorder = recordMidi ? new MidiRecorder() : null;
        if (recordAudio) {
            if (audioCapabilities == null) {
                throw new IOException("FP-30X USB audio input is unavailable");
            }
            audioRecorder = new UsbAudioRecorder(audioCapabilities, audioListener);
            audioTemporary = new File(context.getCacheDir(), stem + ".wav.part");
        } else {
            audioRecorder = null;
            audioTemporary = null;
        }
    }

    public static List<String> outputNames(String stem, boolean audio, boolean midi) {
        ArrayList<String> names = new ArrayList<>();
        if (midi) {
            names.add(stem + ".mid");
        }
        if (audio) {
            names.add(stem + ".wav");
        }
        return Collections.unmodifiableList(names);
    }

    public void start() throws IOException {
        if (!recordAudio && !recordMidi) {
            throw new IOException("Select Audio or MIDI");
        }
        if (recordMidi) {
            if (!connection.canReceiveMidi()) {
                throw new IOException("MIDI input is unavailable");
            }
            midiRecorder.start();
            connection.setIncomingReceiver(midiRecorder);
        }
        try {
            if (recordAudio) {
                audioRecorder.start(audioTemporary);
            }
            started = true;
        } catch (IOException | RuntimeException error) {
            connection.setIncomingReceiver(null);
            throw error;
        }
    }

    public void abort() {
        started = false;
        if (recordMidi) {
            connection.setIncomingReceiver(null);
        }
        if (recordAudio) {
            try {
                audioRecorder.stop();
            } catch (IOException ignored) {
            }
            if (audioTemporary != null) {
                audioTemporary.delete();
            }
        }
    }

    public List<String> stop() throws IOException {
        if (!started) {
            return Collections.emptyList();
        }
        started = false;
        IOException failure = null;
        if (recordMidi) {
            connection.setIncomingReceiver(null);
            try (RecordingFileStore.OutputHandle output = fileStore.create(
                    destination, stem + ".mid", "audio/midi")) {
                midiRecorder.stopAndWrite(output.stream);
                savedNames.add(stem + ".mid");
            } catch (IOException error) {
                failure = error;
            }
        }
        if (recordAudio) {
            try {
                audioRecorder.stop();
                copyAudio();
                savedNames.add(stem + ".wav");
            } catch (IOException error) {
                if (failure == null) {
                    failure = error;
                }
            } finally {
                if (audioTemporary != null) {
                    audioTemporary.delete();
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return Collections.unmodifiableList(new ArrayList<>(savedNames));
    }

    private void copyAudio() throws IOException {
        try (FileInputStream input = new FileInputStream(audioTemporary);
             RecordingFileStore.OutputHandle output = fileStore.create(
                     destination, stem + ".wav", "audio/wav")) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.stream.write(buffer, 0, read);
            }
        }
    }
}

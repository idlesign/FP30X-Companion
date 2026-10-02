package dev.idlesign.fp30xcompanion;

import android.media.midi.MidiReceiver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MidiRecorder extends MidiReceiver {
    public interface Clock {
        long nanoTime();
    }

    private final Clock clock;
    private final ArrayList<StandardMidiFileWriter.Event> events = new ArrayList<>();
    private final byte[] pending = new byte[3];
    private int pendingCount;
    private int expectedCount;
    private int runningStatus;
    private long startNanos;
    private boolean recording;

    public MidiRecorder() {
        this(System::nanoTime);
    }

    MidiRecorder(Clock clock) {
        this.clock = clock;
    }

    public synchronized void start() {
        events.clear();
        pendingCount = 0;
        expectedCount = 0;
        runningStatus = 0;
        startNanos = clock.nanoTime();
        recording = true;
    }

    public synchronized void stopAndWrite(OutputStream output) throws IOException {
        recording = false;
        StandardMidiFileWriter.write(output, startNanos, new ArrayList<>(events));
    }

    public synchronized List<StandardMidiFileWriter.Event> events() {
        return new ArrayList<>(events);
    }

    @Override
    public synchronized void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        if (!recording) {
            return;
        }
        long eventTime = timestamp > 0 ? timestamp : clock.nanoTime();
        int end = offset + count;
        for (int index = offset; index < end; index++) {
            int value = data[index] & 0xFF;
            if (value >= 0xF8) {
                continue;
            }
            if ((value & 0x80) != 0) {
                pendingCount = 0;
                expectedCount = channelLength(value);
                if (expectedCount == 0) {
                    runningStatus = 0;
                    continue;
                }
                runningStatus = value;
                pending[pendingCount++] = (byte) value;
            } else {
                if (pendingCount == 0 && runningStatus != 0) {
                    expectedCount = channelLength(runningStatus);
                    pending[pendingCount++] = (byte) runningStatus;
                }
                if (pendingCount > 0 && pendingCount < expectedCount) {
                    pending[pendingCount++] = (byte) value;
                }
            }
            if (expectedCount > 0 && pendingCount == expectedCount) {
                events.add(new StandardMidiFileWriter.Event(
                        eventTime, Arrays.copyOf(pending, expectedCount)));
                pendingCount = 0;
                expectedCount = runningStatus == 0 ? 0 : channelLength(runningStatus);
            }
        }
    }

    private int channelLength(int status) {
        if (status < 0x80 || status > 0xEF) {
            return 0;
        }
        int kind = status & 0xF0;
        return kind == 0xC0 || kind == 0xD0 ? 2 : 3;
    }
}

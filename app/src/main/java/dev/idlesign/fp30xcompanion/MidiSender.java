package dev.idlesign.fp30xcompanion;

import android.media.midi.MidiInputPort;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MidiSender implements Closeable {
    public interface ErrorListener {
        void onSendError(IOException error);
    }

    private static final int KEYBOARD_CHANNEL = 4;
    private final MidiInputPort port;
    private final ErrorListener errorListener;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean closed;

    public MidiSender(MidiInputPort port, ErrorListener errorListener) {
        this.port = port;
        this.errorListener = errorListener;
    }

    public void sendHandshake() {
        schedule(MidiMessages.appConnectHandshake(), 0);
        schedule(MidiMessages.appConnectHandshake(), 150);
        schedule(MidiMessages.appConnectHandshake(), 450);
    }

    public void applyPerformanceState(PerformanceState state) {
        for (TimedMidiMessage message : MidiPerformanceSequence.build(state)) {
            schedule(message.bytes, message.delayMillis, message.description);
        }
    }

    public void applyMetronomeState(MetronomeState state) {
        schedule(MidiMessages.metronomeTempo(state.tempo), 0, "Metronome tempo");
        schedule(MidiMessages.metronomeBeat(state.beat), 20, "Metronome beat");
        schedule(MidiMessages.metronomeVolume(state.volume), 40, "Metronome volume");
        schedule(MidiMessages.metronomeEnabled(state.enabled), 60, "Metronome switch");
    }

    public void selectTone(Tone tone) {
        if (tone.kind == Tone.Kind.INTERNAL) {
            schedule(MidiMessages.internalTone(tone), 0);
            return;
        }
        schedule(MidiMessages.controlChange(KEYBOARD_CHANNEL, 0, tone.bankMsb), 0);
        schedule(MidiMessages.controlChange(KEYBOARD_CHANNEL, 32, tone.bankLsb), 20);
        schedule(MidiMessages.programChange(KEYBOARD_CHANNEL, tone.program), 40);
        schedule(MidiMessages.noteOn(KEYBOARD_CHANNEL, 60, 1), 120);
        schedule(MidiMessages.noteOff(KEYBOARD_CHANNEL, 60), 130);
    }

    private void schedule(byte[] message, long delayMillis) {
        schedule(message, delayMillis, "MIDI message");
    }

    private void schedule(byte[] message, long delayMillis, String description) {
        executor.schedule(() -> {
            if (closed) {
                return;
            }
            try {
                port.send(message, 0, message.length);
            } catch (IOException error) {
                errorListener.onSendError(new IOException(description + ": " + error.getMessage(), error));
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        executor.shutdownNow();
        port.close();
    }
}

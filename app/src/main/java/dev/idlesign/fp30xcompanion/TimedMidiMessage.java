package dev.idlesign.fp30xcompanion;

import java.util.Arrays;

public final class TimedMidiMessage {
    public final byte[] bytes;
    public final long delayMillis;
    public final String description;

    public TimedMidiMessage(byte[] bytes, long delayMillis, String description) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.delayMillis = delayMillis;
        this.description = description;
    }
}

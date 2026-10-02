package dev.idlesign.fp30xcompanion;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class StandardMidiFileWriter {
    public static final int TICKS_PER_QUARTER = 480;
    public static final int TEMPO_MICROSECONDS = 500_000;

    public record Event(long timestampNanos, byte[] message) {
    }

    private StandardMidiFileWriter() {
    }

    public static void write(OutputStream output, long startNanos, List<Event> events)
            throws IOException {
        ByteArrayOutputStream trackBytes = new ByteArrayOutputStream();
        DataOutputStream track = new DataOutputStream(trackBytes);
        writeVariable(track, 0);
        track.write(new byte[]{(byte) 0xFF, 0x51, 0x03, 0x07, (byte) 0xA1, 0x20});
        long previousTicks = 0;
        for (Event event : events) {
            long ticks = nanosToTicks(Math.max(0, event.timestampNanos() - startNanos));
            writeVariable(track, Math.max(0, ticks - previousTicks));
            track.write(event.message());
            previousTicks = ticks;
        }
        writeVariable(track, 0);
        track.write(new byte[]{(byte) 0xFF, 0x2F, 0x00});
        track.flush();

        DataOutputStream data = new DataOutputStream(output);
        data.writeBytes("MThd");
        data.writeInt(6);
        data.writeShort(0);
        data.writeShort(1);
        data.writeShort(TICKS_PER_QUARTER);
        data.writeBytes("MTrk");
        data.writeInt(trackBytes.size());
        trackBytes.writeTo(data);
        data.flush();
    }

    static long nanosToTicks(long nanos) {
        return Math.round(nanos * (TICKS_PER_QUARTER * 1_000_000.0)
                / (TEMPO_MICROSECONDS * 1_000_000_000.0));
    }

    static void writeVariable(DataOutputStream output, long value) throws IOException {
        long buffer = value & 0x7F;
        while ((value >>= 7) > 0) {
            buffer <<= 8;
            buffer |= (value & 0x7F) | 0x80;
        }
        while (true) {
            output.writeByte((int) buffer);
            if ((buffer & 0x80) != 0) {
                buffer >>= 8;
            } else {
                break;
            }
        }
    }
}

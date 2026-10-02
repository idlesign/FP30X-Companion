package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class StandardMidiFileWriterTest {
    @Test
    public void writesTypeZeroHeaderTempoEventsAndEndOfTrack() throws Exception {
        List<StandardMidiFileWriter.Event> events = Arrays.asList(
                new StandardMidiFileWriter.Event(1_000_000_000L,
                        new byte[]{(byte) 0x90, 60, 100}),
                new StandardMidiFileWriter.Event(1_500_000_000L,
                        new byte[]{(byte) 0x80, 60, 0}),
                new StandardMidiFileWriter.Event(1_600_000_000L,
                        new byte[]{(byte) 0xE0, 0, 64})
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StandardMidiFileWriter.write(output, 1_000_000_000L, events);
        byte[] bytes = output.toByteArray();
        assertEquals("MThd", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        assertEquals(0, bytes[9]);
        assertEquals(1, bytes[11]);
        assertEquals("MTrk", new String(bytes, 14, 4, StandardCharsets.US_ASCII));
        assertTrue(indexOf(bytes, new byte[]{(byte) 0xFF, 0x51, 0x03}) > 0);
        assertTrue(indexOf(bytes, new byte[]{(byte) 0x90, 60, 100}) > 0);
        assertArrayEquals(new byte[]{0, (byte) 0xFF, 0x2F, 0},
                Arrays.copyOfRange(bytes, bytes.length - 4, bytes.length));
    }

    @Test
    public void convertsHalfSecondToOneQuarterNote() {
        assertEquals(480, StandardMidiFileWriter.nanosToTicks(500_000_000L));
    }

    private int indexOf(byte[] data, byte[] target) {
        outer:
        for (int index = 0; index <= data.length - target.length; index++) {
            for (int inner = 0; inner < target.length; inner++) {
                if (data[index + inner] != target[inner]) {
                    continue outer;
                }
            }
            return index;
        }
        return -1;
    }
}

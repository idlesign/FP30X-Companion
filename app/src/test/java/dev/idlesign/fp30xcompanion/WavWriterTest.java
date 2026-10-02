package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

public class WavWriterTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void finalizesStereoPcmHeaderAndDataLength() throws Exception {
        File file = folder.newFile("recording.wav");
        try (WavWriter writer = new WavWriter(file, 48000, 2, 16)) {
            writer.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 0, 8);
            assertEquals(8, writer.pcmBytes());
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        assertEquals("RIFF", new String(bytes, 0, 4));
        assertEquals("WAVE", new String(bytes, 8, 4));
        assertEquals(2, littleShort(bytes, 22));
        assertEquals(48000, littleInt(bytes, 24));
        assertEquals(192000, littleInt(bytes, 28));
        assertEquals(8, littleInt(bytes, 40));
        assertEquals(52, bytes.length);
    }

    @Test
    public void emptyRecordingStillProducesValidHeader() throws Exception {
        File file = folder.newFile("empty.wav");
        try (WavWriter ignored = new WavWriter(file, 44100, 1, 16)) {
        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        assertEquals(44, bytes.length);
        assertEquals(0, littleInt(bytes, 40));
    }

    private int littleShort(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private int littleInt(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }
}

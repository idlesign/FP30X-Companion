package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.TimeZone;

public class RecordingNamesTest {
    @Test
    public void usesRequiredTimestampPatternForSharedStem() {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            RecordingNames names = new RecordingNames(() -> 0L);
            String stem = names.newStem();
            assertEquals("fp30x_19700101_000000", stem);
            assertEquals(stem + ".mid", stem + ".mid");
            assertEquals(stem + ".wav", stem + ".wav");
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}

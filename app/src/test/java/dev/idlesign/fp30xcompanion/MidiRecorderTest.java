package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MidiRecorderTest {
    @Test
    public void recordsFragmentedPacketsRunningStatusAndPedal() throws Exception {
        Clock clock = new Clock();
        MidiRecorder recorder = new MidiRecorder(clock);
        recorder.start();
        recorder.onSend(new byte[]{(byte) 0x93, 60}, 0, 2, 1_100_000_000L);
        recorder.onSend(new byte[]{100, 61, 90, (byte) 0xFE}, 0, 4, 1_200_000_000L);
        recorder.onSend(new byte[]{(byte) 0xB3, 64, 127}, 0, 3, 1_300_000_000L);

        List<StandardMidiFileWriter.Event> events = recorder.events();
        assertEquals(3, events.size());
        assertArrayEquals(new byte[]{(byte) 0x93, 60, 100}, events.get(0).message());
        assertArrayEquals(new byte[]{(byte) 0x93, 61, 90}, events.get(1).message());
        assertArrayEquals(new byte[]{(byte) 0xB3, 64, 127}, events.get(2).message());
    }

    @Test
    public void ignoresInputOutsideRecordingAndCanRepeatSessions() throws Exception {
        Clock clock = new Clock();
        MidiRecorder recorder = new MidiRecorder(clock);
        recorder.onSend(new byte[]{(byte) 0x90, 60, 1}, 0, 3, 0);
        assertEquals(0, recorder.events().size());
        recorder.start();
        recorder.onSend(new byte[]{(byte) 0x90, 60, 1}, 0, 3, 0);
        recorder.stopAndWrite(new ByteArrayOutputStream());
        recorder.start();
        assertEquals(0, recorder.events().size());
    }

    private static final class Clock implements MidiRecorder.Clock {
        long value = 1_000_000_000L;

        @Override
        public long nanoTime() {
            return value;
        }
    }
}

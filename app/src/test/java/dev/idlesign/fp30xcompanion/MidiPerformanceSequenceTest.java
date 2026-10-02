package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class MidiPerformanceSequenceTest {
    @Test
    public void singleStartsWithModeThenToneAndCommonSettings() {
        PerformanceState state = state(internal("Concert Piano", 0, 0));
        List<TimedMidiMessage> messages = MidiPerformanceSequence.build(state);
        assertEquals("keyboard mode", messages.get(0).description);
        assertArrayEquals(MidiMessages.keyboardMode(PerformanceMode.SINGLE), messages.get(0).bytes);
        assertEquals("Single tone", messages.get(1).description);
        assertArrayEquals(MidiMessages.internalTone(state.primaryTone), messages.get(1).bytes);
        assertEquals("transpose", messages.get(2).description);
        assertTrue(messages.get(messages.size() - 1).delayMillis > messages.get(0).delayMillis);
    }

    @Test
    public void dualUsesMainAndLayerAddresses() {
        Tone main = internal("Concert Piano", 0, 0);
        Tone layer = internal("Epic Strings", 2, 1);
        List<TimedMidiMessage> messages = MidiPerformanceSequence.build(state(main).assignLayer(layer));
        assertArrayEquals(MidiMessages.keyboardMode(PerformanceMode.DUAL), messages.get(0).bytes);
        assertArrayEquals(MidiMessages.internalTone(main), messages.get(1).bytes);
        assertArrayEquals(MidiMessages.dualTone(layer), messages.get(2).bytes);
        assertEquals("dual balance", messages.get(3).description);
    }

    @Test
    public void splitUsesRightAndLeftAddresses() {
        Tone right = internal("Concert Piano", 0, 0);
        Tone left = internal("Acoustic Bass", 2, 4);
        List<TimedMidiMessage> messages = MidiPerformanceSequence.build(state(right).assignLeft(left));
        assertArrayEquals(MidiMessages.keyboardMode(PerformanceMode.SPLIT), messages.get(0).bytes);
        assertArrayEquals(MidiMessages.internalTone(right), messages.get(1).bytes);
        assertArrayEquals(MidiMessages.splitTone(left), messages.get(2).bytes);
        assertEquals("split point", messages.get(3).description);
    }

    @Test
    public void bankProgramUsesChannelsFourAndTwoWithLatch() {
        Tone primary = bank("Sax", 16, 67, 66);
        Tone layer = bank("Choir", 121, 0, 53);
        List<TimedMidiMessage> messages = MidiPerformanceSequence.build(state(primary).assignLayer(layer));
        assertEquals((byte) 0xB3, messages.get(1).bytes[0]);
        assertEquals((byte) 0xC3, messages.get(3).bytes[0]);
        assertEquals((byte) 0x93, messages.get(4).bytes[0]);
        assertEquals((byte) 0xB1, messages.get(6).bytes[0]);
        assertEquals((byte) 0xC1, messages.get(8).bytes[0]);
        assertEquals((byte) 0x91, messages.get(9).bytes[0]);
        assertTrue(messages.get(4).delayMillis - messages.get(3).delayMillis >= 80);
    }

    private PerformanceState state(Tone tone) {
        return new PerformanceState(tone, null, null, 9, 54, 0, 0, 0, 3, 1, 0);
    }

    private Tone internal(String name, int category, int number) {
        return new Tone(number + 1, "Other", name, 0, 68, number + 1,
                Tone.Kind.INTERNAL, category, number);
    }

    private Tone bank(String name, int msb, int lsb, int program) {
        return new Tone(program, "GM2", name, msb, lsb, program,
                Tone.Kind.BANK_PROGRAM, 0, 0);
    }
}

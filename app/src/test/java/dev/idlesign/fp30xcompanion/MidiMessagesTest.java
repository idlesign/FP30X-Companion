package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class MidiMessagesTest {
    @Test
    public void bankAndProgramUseKeyboardChannelFour() {
        assertArrayEquals(
                new byte[]{(byte) 0xB3, 0, 16},
                MidiMessages.controlChange(4, 0, 16)
        );
        assertArrayEquals(
                new byte[]{(byte) 0xB3, 32, 67},
                MidiMessages.controlChange(4, 32, 67)
        );
        assertArrayEquals(
                new byte[]{(byte) 0xC3, 0},
                MidiMessages.programChange(4, 1)
        );
    }

    @Test
    public void handshakeMatchesRolandDt1() {
        assertArrayEquals(
                new byte[]{
                        (byte) 0xF0, 0x41, 0x10, 0, 0, 0, 0x28, 0x12,
                        1, 0, 3, 6, 1, 0x75, (byte) 0xF7,
                },
                MidiMessages.appConnectHandshake()
        );
    }

    @Test
    public void keyboardModeAndRq1UseRolandChecksum() {
        assertArrayEquals(
                new byte[]{
                        (byte) 0xF0, 0x41, 0x10, 0, 0, 0, 0x28, 0x12,
                        1, 0, 2, 0, 2, 0x7B, (byte) 0xF7,
                },
                MidiMessages.keyboardMode(PerformanceMode.DUAL)
        );
        assertArrayEquals(
                new byte[]{
                        (byte) 0xF0, 0x41, 0x10, 0, 0, 0, 0x28, 0x11,
                        1, 0, 2, 0, 0, 0, 0, 1, 0x7C, (byte) 0xF7,
                },
                MidiMessages.rolandRq1(
                        new int[]{1, 0, 2, 0}, new int[]{0, 0, 0, 1})
        );
    }

    @Test
    public void settingsUseExpectedAddressesAndEncodings() {
        assertArrayEquals(
                MidiMessages.rolandDt1(new int[]{1, 0, 2, 3}, new int[]{64}),
                MidiMessages.splitBalance(9)
        );
        assertArrayEquals(
                MidiMessages.rolandDt1(new int[]{1, 0, 2, 5}, new int[]{64}),
                MidiMessages.dualBalance(9)
        );
        assertArrayEquals(
                new byte[]{(byte) 0xF0, 0x7F, 0x7F, 4, 4, 0, 59, (byte) 0xF7},
                MidiMessages.masterCoarseTuning(-5)
        );
    }

    @Test
    public void internalToneUsesSingleToneAddressAndChecksum() {
        Tone tone = new Tone(1, "Piano", "Concert Piano", 0, 68, 1,
                Tone.Kind.INTERNAL, 0, 0);
        assertArrayEquals(
                new byte[]{
                        (byte) 0xF0, 0x41, 0x10, 0, 0, 0, 0x28, 0x12,
                        1, 0, 2, 7, 0, 0, 0, 0x76, (byte) 0xF7,
                },
                MidiMessages.internalTone(tone)
        );
    }
}

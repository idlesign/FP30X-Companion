package dev.idlesign.fp30xcompanion;

import java.io.ByteArrayOutputStream;

public final class MidiMessages {
    private MidiMessages() {
    }

    public static byte[] appConnectHandshake() {
        return rolandDt1(new int[]{0x01, 0x00, 0x03, 0x06}, new int[]{0x01});
    }

    public static byte[] internalTone(Tone tone) {
        if (tone.kind != Tone.Kind.INTERNAL) {
            throw new IllegalArgumentException("Tone is not internal");
        }
        return rolandDt1(
                new int[]{0x01, 0x00, 0x02, 0x07},
                new int[]{tone.sysExCategory, tone.sysExNumber / 128, tone.sysExNumber % 128}
        );
    }

    public static byte[] controlChange(int channelOneBased, int controller, int value) {
        require7Bit(controller);
        require7Bit(value);
        return new byte[]{
                (byte) (0xB0 | channelZeroBased(channelOneBased)),
                (byte) controller,
                (byte) value,
        };
    }

    public static byte[] programChange(int channelOneBased, int programDocumentValue) {
        if (programDocumentValue < 1 || programDocumentValue > 128) {
            throw new IllegalArgumentException("Program must be in 1..128");
        }
        return new byte[]{
                (byte) (0xC0 | channelZeroBased(channelOneBased)),
                (byte) (programDocumentValue - 1),
        };
    }

    public static byte[] noteOn(int channelOneBased, int note, int velocity) {
        require7Bit(note);
        require7Bit(velocity);
        return new byte[]{
                (byte) (0x90 | channelZeroBased(channelOneBased)),
                (byte) note,
                (byte) velocity,
        };
    }

    public static byte[] noteOff(int channelOneBased, int note) {
        require7Bit(note);
        return new byte[]{
                (byte) (0x80 | channelZeroBased(channelOneBased)),
                (byte) note,
                0,
        };
    }

    public static byte[] keyboardMode(PerformanceMode mode) {
        int value = switch (mode) {
            case SINGLE -> 0;
            case SPLIT -> 1;
            case DUAL -> 2;
        };
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x00}, new int[]{value});
    }

    public static byte[] splitTone(Tone tone) {
        return internalToneAt(tone, 0x0A);
    }

    public static byte[] dualTone(Tone tone) {
        return internalToneAt(tone, 0x0D);
    }

    public static byte[] splitPoint(int note) {
        requireRange("Split point", note, 0, 127);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x01}, new int[]{note});
    }

    public static byte[] splitBalance(int value) {
        requireRange("Split balance", value, 0, 18);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x03}, new int[]{64 + value - 9});
    }

    public static byte[] dualBalance(int value) {
        requireRange("Dual balance", value, 0, 18);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x05}, new int[]{64 + (value - 9) * 3});
    }

    public static byte[] splitLeftOctaveShift(int value) {
        return octaveShiftAt(0x02, value);
    }

    public static byte[] splitRightOctaveShift(int value) {
        return octaveShiftAt(0x16, value);
    }

    public static byte[] dualLayerOctaveShift(int value) {
        return octaveShiftAt(0x04, value);
    }

    public static byte[] dualMainOctaveShift(int value) {
        return octaveShiftAt(0x17, value);
    }

    public static byte[] masterCoarseTuning(int semitones) {
        requireRange("Transpose", semitones, -24, 24);
        return new byte[]{
                (byte) 0xF0, 0x7F, 0x7F, 0x04, 0x04, 0x00,
                (byte) (64 + semitones), (byte) 0xF7,
        };
    }

    public static byte[] keyTouch(int value) {
        requireRange("Key touch", value, 0, 5);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x1D}, new int[]{value});
    }

    public static byte[] ambience(int value) {
        requireRange("Ambience", value, 0, 10);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x1A}, new int[]{value});
    }

    public static byte[] brilliance(int value) {
        requireRange("Brilliance", value, -1, 1);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x1C}, new int[]{64 + value});
    }

    public static byte[] metronomeEnabled(boolean enabled) {
        return rolandDt1(new int[]{0x01, 0x00, 0x03, 0x1A},
                new int[]{enabled ? 1 : 0});
    }

    public static byte[] metronomeTempo(int bpm) {
        requireRange("Metronome tempo", bpm, 10, 500);
        return rolandDt1(new int[]{0x01, 0x00, 0x03, 0x09},
                new int[]{bpm / 128, bpm % 128});
    }

    public static byte[] metronomeBeat(int beat) {
        requireRange("Metronome beat", beat, 0, 5);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x1F}, new int[]{beat});
    }

    public static byte[] metronomeVolume(int volume) {
        requireRange("Metronome volume", volume, 0, 10);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, 0x21}, new int[]{volume});
    }

    public static byte[] rolandRq1(int[] address, int[] size) {
        return rolandMessage(0x11, address, size);
    }

    public static byte[] rolandDt1(int[] address, int[] data) {
        return rolandMessage(0x12, address, data);
    }

    private static byte[] internalToneAt(Tone tone, int addressLastByte) {
        if (tone.kind != Tone.Kind.INTERNAL) {
            throw new IllegalArgumentException("Tone is not internal");
        }
        return rolandDt1(
                new int[]{0x01, 0x00, 0x02, addressLastByte},
                new int[]{tone.sysExCategory, tone.sysExNumber / 128, tone.sysExNumber % 128}
        );
    }

    private static byte[] octaveShiftAt(int addressLastByte, int value) {
        requireRange("Octave shift", value, -3, 3);
        return rolandDt1(new int[]{0x01, 0x00, 0x02, addressLastByte}, new int[]{64 + value});
    }

    private static byte[] rolandMessage(int command, int[] address, int[] data) {
        if (address.length != 4) {
            throw new IllegalArgumentException("Roland address must contain four bytes");
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(0xF0);
        result.write(0x41);
        result.write(0x10);
        result.write(0x00);
        result.write(0x00);
        result.write(0x00);
        result.write(0x28);
        result.write(command);
        int sum = 0;
        for (int value : address) {
            require7Bit(value);
            result.write(value);
            sum += value;
        }
        for (int value : data) {
            require7Bit(value);
            result.write(value);
            sum += value;
        }
        result.write((128 - (sum % 128)) % 128);
        result.write(0xF7);
        return result.toByteArray();
    }

    private static int channelZeroBased(int channelOneBased) {
        if (channelOneBased < 1 || channelOneBased > 16) {
            throw new IllegalArgumentException("Channel must be in 1..16");
        }
        return channelOneBased - 1;
    }

    private static void requireRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in " + minimum + ".." + maximum);
        }
    }

    private static void require7Bit(int value) {
        if (value < 0 || value > 127) {
            throw new IllegalArgumentException("MIDI value must be in 0..127");
        }
    }
}

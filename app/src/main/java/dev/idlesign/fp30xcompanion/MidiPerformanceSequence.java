package dev.idlesign.fp30xcompanion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MidiPerformanceSequence {
    private static final int PRIMARY_CHANNEL = 4;
    private static final int SECONDARY_CHANNEL = 2;
    private static final long GAP = 30;

    private MidiPerformanceSequence() {
    }

    public static List<TimedMidiMessage> build(PerformanceState state) {
        Builder result = new Builder();
        result.add(MidiMessages.keyboardMode(state.mode()), "keyboard mode");
        addTone(result, state.primaryTone, PRIMARY_CHANNEL, ToneRole.PRIMARY, state.mode());
        if (state.mode() == PerformanceMode.DUAL) {
            addTone(result, state.layerTone, SECONDARY_CHANNEL, ToneRole.LAYER, state.mode());
            result.add(MidiMessages.dualBalance(state.balance), "dual balance");
            result.add(MidiMessages.dualMainOctaveShift(state.primaryOctaveShift), "dual main octave");
            result.add(MidiMessages.dualLayerOctaveShift(state.secondaryOctaveShift), "dual layer octave");
        } else if (state.mode() == PerformanceMode.SPLIT) {
            addTone(result, state.leftTone, SECONDARY_CHANNEL, ToneRole.LEFT, state.mode());
            result.add(MidiMessages.splitPoint(state.splitPoint), "split point");
            result.add(MidiMessages.splitBalance(state.balance), "split balance");
            result.add(MidiMessages.splitRightOctaveShift(state.primaryOctaveShift), "split right octave");
            result.add(MidiMessages.splitLeftOctaveShift(state.secondaryOctaveShift), "split left octave");
        }
        result.add(MidiMessages.masterCoarseTuning(state.transpose), "transpose");
        result.add(MidiMessages.keyTouch(state.keyTouch), "key touch");
        result.add(MidiMessages.ambience(state.ambience), "ambience");
        result.add(MidiMessages.brilliance(state.brilliance), "brilliance");
        return Collections.unmodifiableList(result.messages);
    }

    private static void addTone(
            Builder result,
            Tone tone,
            int channel,
            ToneRole role,
            PerformanceMode mode
    ) {
        if (tone.kind == Tone.Kind.INTERNAL) {
            byte[] message;
            if (role == ToneRole.LAYER) {
                message = MidiMessages.dualTone(tone);
            } else if (role == ToneRole.LEFT) {
                message = MidiMessages.splitTone(tone);
            } else {
                message = MidiMessages.internalTone(tone);
            }
            result.add(message, role.displayName(mode) + " tone");
            return;
        }
        result.add(MidiMessages.controlChange(channel, 0, tone.bankMsb), "bank MSB");
        result.add(MidiMessages.controlChange(channel, 32, tone.bankLsb), "bank LSB");
        result.add(MidiMessages.programChange(channel, tone.program), "program change");
        result.addAfter(80, MidiMessages.noteOn(channel, 60, 1), "tone latch on");
        result.addAfter(10, MidiMessages.noteOff(channel, 60), "tone latch off");
    }

    private static final class Builder {
        final ArrayList<TimedMidiMessage> messages = new ArrayList<>();
        long delay;

        void add(byte[] bytes, String description) {
            messages.add(new TimedMidiMessage(bytes, delay, description));
            delay += GAP;
        }

        void addAfter(long additionalDelay, byte[] bytes, String description) {
            delay += additionalDelay;
            add(bytes, description);
        }
    }
}

package dev.idlesign.fp30xcompanion;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PianoSettingsTest {
    @Test
    public void modesEnableOnlyApplicableControls() {
        PianoSettingsAvailability single = PianoSettingsAvailability.forMode(PerformanceMode.SINGLE);
        assertFalse(single.balance);
        assertFalse(single.splitPoint);
        assertFalse(single.secondaryOctave);

        PianoSettingsAvailability dual = PianoSettingsAvailability.forMode(PerformanceMode.DUAL);
        assertTrue(dual.balance);
        assertFalse(dual.splitPoint);
        assertTrue(dual.secondaryOctave);

        PianoSettingsAvailability split = PianoSettingsAvailability.forMode(PerformanceMode.SPLIT);
        assertTrue(split.balance);
        assertTrue(split.splitPoint);
        assertTrue(split.secondaryOctave);
    }

    @Test
    public void negativeLogicalRangesAreMappedToNonNegativePickerIndices() {
        assertTrue(PianoSettingsDialog.pickerIndex(-6, -6) == 0);
        assertTrue(PianoSettingsDialog.pickerIndex(-6, 5) == 11);
        assertTrue(PianoSettingsDialog.pickerValue(-6, 0) == -6);
        assertTrue(PianoSettingsDialog.pickerValue(-6, 11) == 5);
    }

    @Test
    public void metronomeMessagesUseVerifiedRolandAddresses() {
        assertArrayEquals(MidiMessages.rolandDt1(
                new int[]{1, 0, 3, 0x1A}, new int[]{1}),
                MidiMessages.metronomeEnabled(true));
        assertArrayEquals(MidiMessages.rolandDt1(
                new int[]{1, 0, 3, 9}, new int[]{0, 120}),
                MidiMessages.metronomeTempo(120));
        assertArrayEquals(MidiMessages.rolandDt1(
                new int[]{1, 0, 2, 0x1F}, new int[]{3}),
                MidiMessages.metronomeBeat(3));
        assertArrayEquals(MidiMessages.rolandDt1(
                new int[]{1, 0, 2, 0x21}, new int[]{5}),
                MidiMessages.metronomeVolume(5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tempoRejectsOutOfRangeValue() {
        MidiMessages.metronomeTempo(501);
    }

    @Test(expected = IllegalArgumentException.class)
    public void metronomeStateRejectsUnknownBeat() {
        new MetronomeState(false, 120, 6, 5);
    }

    @Test
    public void splitPointUsesReadableNoteNames() {
        assertTrue(PianoSettingsDialog.noteName(54).startsWith("F♯3"));
    }
}

package dev.idlesign.fp30xcompanion;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RecordingPresentationTest {
    @Test
    public void disconnectedDisablesEverySource() {
        RecordingPresentation state = RecordingPresentation.from(
                false, ConnectionTarget.USB, true, true, false);
        assertFalse(state.midiEnabled);
        assertFalse(state.audioEnabled);
        assertFalse(state.disconnectEnabled);
    }

    @Test
    public void bluetoothOffersMidiOnly() {
        RecordingPresentation state = RecordingPresentation.from(
                true, ConnectionTarget.BLUETOOTH_MIDI, true, true, false);
        assertTrue(state.midiEnabled);
        assertFalse(state.audioEnabled);
        assertTrue(state.disconnectEnabled);
    }

    @Test
    public void usbReflectsActualMidiAndAudioCapabilities() {
        RecordingPresentation midiOnly = RecordingPresentation.from(
                true, ConnectionTarget.USB, true, false, false);
        assertTrue(midiOnly.midiEnabled);
        assertFalse(midiOnly.audioEnabled);

        RecordingPresentation both = RecordingPresentation.from(
                true, ConnectionTarget.USB, true, true, false);
        assertTrue(both.midiEnabled);
        assertTrue(both.audioEnabled);
    }

    @Test
    public void recordingLocksSourcesAndDisconnect() {
        RecordingPresentation state = RecordingPresentation.from(
                true, ConnectionTarget.USB, true, true, true);
        assertFalse(state.midiEnabled);
        assertFalse(state.audioEnabled);
        assertFalse(state.disconnectEnabled);
    }
}

package dev.idlesign.fp30xcompanion;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class PresetSettingsTest {
    @Test
    public void performanceSettingsRoundTripButMetronomeIsNotSerialized() throws Exception {
        Tone piano = new Tone(1, "Piano", "Concert Piano", 0, 68, 1,
                Tone.Kind.INTERNAL, 0, 0);
        PerformanceState state = new PerformanceState(
                piano, null, null, 7, 61, -1, 2, -3, 4, 8, 1);

        JSONObject json = PerformanceStateJson.toJson(state);
        PerformanceState restored = PerformanceStateJson.fromJson(json,
                Collections.singletonList(piano));

        assertEquals(7, restored.balance);
        assertEquals(61, restored.splitPoint);
        assertEquals(-1, restored.primaryOctaveShift);
        assertEquals(2, restored.secondaryOctaveShift);
        assertEquals(-3, restored.transpose);
        assertEquals(4, restored.keyTouch);
        assertEquals(8, restored.ambience);
        assertEquals(1, restored.brilliance);
        assertFalse(json.has("metronome"));
        assertFalse(json.has("connection"));
        assertFalse(json.has("recording"));
    }
}

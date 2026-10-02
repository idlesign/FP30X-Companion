package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class PerformanceStateJsonTest {
    private final Tone piano = tone(1, "Concert Piano");
    private final Tone strings = tone(2, "Epic Strings");
    private final Tone bass = tone(3, "Acoustic Bass");
    private final List<Tone> tones = Arrays.asList(piano, strings, bass);

    @Test
    public void roundTripsSingleDualAndSplit() throws Exception {
        assertRoundTrip(state(piano));
        assertRoundTrip(state(piano).assignLayer(strings));
        assertRoundTrip(state(piano).assignLeft(bass));
    }

    @Test
    public void missingOptionalFieldsUseDefaults() throws Exception {
        JSONObject raw = new JSONObject().put("primary", piano.stableId());
        PerformanceState result = PerformanceStateJson.fromJson(raw, tones);
        assertSame(piano, result.primaryTone);
        assertNull(result.layerTone);
        assertNull(result.leftTone);
        assertEquals(PerformanceDefaults.BALANCE, result.balance);
        assertEquals(PerformanceDefaults.KEY_TOUCH, result.keyTouch);
    }

    private void assertRoundTrip(PerformanceState original) throws Exception {
        PerformanceState restored = PerformanceStateJson.fromJson(
                PerformanceStateJson.toJson(original), tones);
        assertEquals(original.mode(), restored.mode());
        assertEquals(original.primaryTone.stableId(), restored.primaryTone.stableId());
        assertEquals(id(original.layerTone), id(restored.layerTone));
        assertEquals(id(original.leftTone), id(restored.leftTone));
        assertEquals(original.balance, restored.balance);
        assertEquals(original.splitPoint, restored.splitPoint);
        assertEquals(original.transpose, restored.transpose);
    }

    private String id(Tone tone) {
        return tone == null ? null : tone.stableId();
    }

    private PerformanceState state(Tone primary) {
        return new PerformanceState(primary, null, null, 9, 54, 0, 0, 0, 3, 1, 0);
    }

    private Tone tone(int number, String name) {
        return new Tone(number, number == 1 ? "Piano" : "Other", name, 0, 68, number,
                Tone.Kind.INTERNAL, number == 1 ? 0 : 2, number - 1);
    }
}

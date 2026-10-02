package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;

public class PerformanceStateTest {
    private final Tone piano = tone(1, "Concert Piano");
    private final Tone strings = tone(2, "Epic Strings");
    private final Tone bass = tone(3, "Acoustic Bass");

    @Test
    public void derivesModesAndPrimaryLabels() {
        PerformanceState single = state(piano);
        assertEquals(PerformanceMode.SINGLE, single.mode());
        assertEquals("Single", ToneRole.PRIMARY.displayName(single.mode()));

        PerformanceState dual = single.assignLayer(strings);
        assertEquals(PerformanceMode.DUAL, dual.mode());
        assertEquals("Main", ToneRole.PRIMARY.displayName(dual.mode()));
        assertEquals("Layer", ToneRole.LAYER.displayName(dual.mode()));

        PerformanceState split = single.assignLeft(bass);
        assertEquals(PerformanceMode.SPLIT, split.mode());
        assertEquals("Right", ToneRole.PRIMARY.displayName(split.mode()));
        assertEquals("Left", ToneRole.LEFT.displayName(split.mode()));
    }

    @Test
    public void switchesBetweenDualAndSplitWithoutInvalidCombination() {
        PerformanceState dual = state(piano).assignLayer(strings);
        PerformanceState split = dual.assignLeft(bass);
        assertEquals(PerformanceMode.SPLIT, split.mode());
        assertSame(piano, split.primaryTone);
        assertSame(bass, split.leftTone);
        assertNull(split.layerTone);

        PerformanceState dualAgain = split.assignLayer(strings);
        assertEquals(PerformanceMode.DUAL, dualAgain.mode());
        assertSame(strings, dualAgain.layerTone);
        assertNull(dualAgain.leftTone);
    }

    @Test
    public void assigningSingleClearsSecondaryTone() {
        PerformanceState result = state(piano).assignLayer(strings).assignSingle(bass);
        assertEquals(PerformanceMode.SINGLE, result.mode());
        assertSame(bass, result.primaryTone);
        assertNull(result.layerTone);
        assertNull(result.leftTone);
    }

    @Test
    public void removingSecondaryReturnsToSingle() {
        PerformanceState fromDual = state(piano).assignLayer(strings)
                .remove(ToneRole.LAYER, piano);
        assertEquals(PerformanceMode.SINGLE, fromDual.mode());
        assertSame(piano, fromDual.primaryTone);

        PerformanceState fromSplit = state(piano).assignLeft(bass)
                .remove(ToneRole.LEFT, piano);
        assertEquals(PerformanceMode.SINGLE, fromSplit.mode());
        assertSame(piano, fromSplit.primaryTone);
    }

    @Test
    public void removingPrimaryPromotesSecondaryOrUsesFallback() {
        PerformanceState dual = state(piano).assignLayer(strings)
                .remove(ToneRole.PRIMARY, piano);
        assertEquals(PerformanceMode.SINGLE, dual.mode());
        assertSame(strings, dual.primaryTone);

        PerformanceState split = state(piano).assignLeft(bass)
                .remove(ToneRole.PRIMARY, piano);
        assertEquals(PerformanceMode.SINGLE, split.mode());
        assertSame(bass, split.primaryTone);

        PerformanceState single = state(strings).remove(ToneRole.PRIMARY, piano);
        assertSame(piano, single.primaryTone);
    }

    @Test
    public void defaultsUseConcertPianoAndValidSettings() {
        PerformanceState defaults = PerformanceDefaults.fromCatalog(Arrays.asList(strings, piano));
        assertEquals(PerformanceMode.SINGLE, defaults.mode());
        assertSame(piano, defaults.primaryTone);
        assertEquals(PerformanceDefaults.BALANCE, defaults.balance);
        assertEquals(PerformanceDefaults.SPLIT_POINT, defaults.splitPoint);
        assertEquals(PerformanceDefaults.KEY_TOUCH, defaults.keyTouch);
    }

    private PerformanceState state(Tone primary) {
        return new PerformanceState(primary, null, null, 9, 54, 0, 0, 0, 3, 1, 0);
    }

    private Tone tone(int number, String name) {
        return new Tone(number, "Other", name, 0, 0, number,
                Tone.Kind.INTERNAL, 2, number - 1);
    }
}

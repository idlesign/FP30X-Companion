package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class PianoPresentationTest {
    private final Tone piano = tone(1, "Concert Piano");
    private final Tone strings = tone(2, "Epic Strings");
    private final Tone bass = tone(3, "Acoustic Bass");

    @Test
    public void primaryRoleChangesWithMode() {
        assertEquals("Single", rows(state(piano)).get(0).roleLabel());
        assertEquals("Main", rows(state(piano).assignLayer(strings)).get(0).roleLabel());
        assertEquals("Right", rows(state(piano).assignLeft(bass)).get(0).roleLabel());
    }

    @Test
    public void compactRowsContainOnlyAssignedTones() {
        List<PianoPresentation.AssignedTone> dual = rows(state(piano).assignLayer(strings));
        assertEquals(2, dual.size());
        assertEquals("Concert Piano", dual.get(0).tone().name);
        assertEquals("Epic Strings", dual.get(1).tone().name);
        assertEquals("Layer", dual.get(1).roleLabel());
    }

    @Test
    public void allModesOfferSinglePrimaryLayerAndLeftAssignments() {
        for (PerformanceMode mode : PerformanceMode.values()) {
            List<PianoPresentation.AssignmentAction> actions =
                    PianoPresentation.assignmentActions(mode);
            assertEquals(4, actions.size());
            assertTrue(actions.contains(PianoPresentation.AssignmentAction.SINGLE));
            assertTrue(actions.contains(PianoPresentation.AssignmentAction.PRIMARY));
            assertTrue(actions.contains(PianoPresentation.AssignmentAction.LAYER));
            assertTrue(actions.contains(PianoPresentation.AssignmentAction.LEFT));
        }
    }

    private List<PianoPresentation.AssignedTone> rows(PerformanceState state) {
        return PianoPresentation.assignedTones(state);
    }

    private PerformanceState state(Tone primary) {
        return new PerformanceState(primary, null, null, 9, 54, 0, 0, 0, 3, 1, 0);
    }

    private Tone tone(int number, String name) {
        return new Tone(number, "Other", name, 0, 68, number,
                Tone.Kind.INTERNAL, 2, number - 1);
    }
}

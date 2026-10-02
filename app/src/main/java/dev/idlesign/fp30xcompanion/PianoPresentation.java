package dev.idlesign.fp30xcompanion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PianoPresentation {
    public enum AssignmentAction {
        SINGLE,
        PRIMARY,
        LAYER,
        LEFT
    }

    public record AssignedTone(Tone tone, ToneRole role, String roleLabel) {
    }

    private PianoPresentation() {
    }

    public static List<AssignedTone> assignedTones(PerformanceState state) {
        ArrayList<AssignedTone> result = new ArrayList<>();
        result.add(new AssignedTone(
                state.primaryTone,
                ToneRole.PRIMARY,
                ToneRole.PRIMARY.displayName(state.mode())));
        if (state.layerTone != null) {
            result.add(new AssignedTone(state.layerTone, ToneRole.LAYER, "Layer"));
        }
        if (state.leftTone != null) {
            result.add(new AssignedTone(state.leftTone, ToneRole.LEFT, "Left"));
        }
        return Collections.unmodifiableList(result);
    }

    public static List<AssignmentAction> assignmentActions(PerformanceMode mode) {
        ArrayList<AssignmentAction> result = new ArrayList<>();
        result.add(AssignmentAction.SINGLE);
        result.add(AssignmentAction.PRIMARY);
        if (mode == PerformanceMode.SPLIT) {
            result.add(AssignmentAction.LEFT);
            result.add(AssignmentAction.LAYER);
        } else {
            result.add(AssignmentAction.LAYER);
            result.add(AssignmentAction.LEFT);
        }
        return Collections.unmodifiableList(result);
    }
}

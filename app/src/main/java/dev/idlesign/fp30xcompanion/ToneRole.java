package dev.idlesign.fp30xcompanion;

public enum ToneRole {
    PRIMARY,
    LAYER,
    LEFT;

    public String displayName(PerformanceMode mode) {
        if (this == LAYER) {
            return "Layer";
        }
        if (this == LEFT) {
            return "Left";
        }
        return switch (mode) {
            case SINGLE -> "Single";
            case DUAL -> "Main";
            case SPLIT -> "Right";
        };
    }
}

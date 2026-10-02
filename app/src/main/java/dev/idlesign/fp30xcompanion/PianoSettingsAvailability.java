package dev.idlesign.fp30xcompanion;

public final class PianoSettingsAvailability {
    public final boolean balance;
    public final boolean splitPoint;
    public final boolean secondaryOctave;

    private PianoSettingsAvailability(boolean balance, boolean splitPoint, boolean secondaryOctave) {
        this.balance = balance;
        this.splitPoint = splitPoint;
        this.secondaryOctave = secondaryOctave;
    }

    public static PianoSettingsAvailability forMode(PerformanceMode mode) {
        boolean secondary = mode != PerformanceMode.SINGLE;
        return new PianoSettingsAvailability(
                secondary,
                mode == PerformanceMode.SPLIT,
                secondary
        );
    }
}

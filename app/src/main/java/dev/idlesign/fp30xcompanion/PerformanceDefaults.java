package dev.idlesign.fp30xcompanion;

import java.util.List;

public final class PerformanceDefaults {
    public static final int BALANCE = 9;
    public static final int SPLIT_POINT = 54;
    public static final int OCTAVE_SHIFT = 0;
    public static final int TRANSPOSE = 0;
    public static final int KEY_TOUCH = 3;
    public static final int AMBIENCE = 1;
    public static final int BRILLIANCE = 0;

    private PerformanceDefaults() {
    }

    public static PerformanceState fromCatalog(List<Tone> tones) {
        Tone concertPiano = ToneCatalog.findByName(tones, "Concert Piano");
        if (concertPiano == null) {
            throw new IllegalArgumentException("Concert Piano is missing from the tone catalog");
        }
        return new PerformanceState(
                concertPiano,
                null,
                null,
                BALANCE,
                SPLIT_POINT,
                OCTAVE_SHIFT,
                OCTAVE_SHIFT,
                TRANSPOSE,
                KEY_TOUCH,
                AMBIENCE,
                BRILLIANCE
        );
    }
}

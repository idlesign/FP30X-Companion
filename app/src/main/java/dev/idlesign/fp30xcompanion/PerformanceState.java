package dev.idlesign.fp30xcompanion;

import java.util.Objects;

public final class PerformanceState {
    public final Tone primaryTone;
    public final Tone layerTone;
    public final Tone leftTone;
    public final int balance;
    public final int splitPoint;
    public final int primaryOctaveShift;
    public final int secondaryOctaveShift;
    public final int transpose;
    public final int keyTouch;
    public final int ambience;
    public final int brilliance;

    public PerformanceState(
            Tone primaryTone,
            Tone layerTone,
            Tone leftTone,
            int balance,
            int splitPoint,
            int primaryOctaveShift,
            int secondaryOctaveShift,
            int transpose,
            int keyTouch,
            int ambience,
            int brilliance
    ) {
        this.primaryTone = Objects.requireNonNull(primaryTone);
        if (layerTone != null && leftTone != null) {
            throw new IllegalArgumentException("Dual and Split secondary tones are mutually exclusive");
        }
        this.layerTone = layerTone;
        this.leftTone = leftTone;
        this.balance = requireRange("balance", balance, 0, 18);
        this.splitPoint = requireRange("splitPoint", splitPoint, 0, 127);
        this.primaryOctaveShift = requireRange("primaryOctaveShift", primaryOctaveShift, -3, 3);
        this.secondaryOctaveShift = requireRange("secondaryOctaveShift", secondaryOctaveShift, -3, 3);
        this.transpose = requireRange("transpose", transpose, -6, 5);
        this.keyTouch = requireRange("keyTouch", keyTouch, 0, 5);
        this.ambience = requireRange("ambience", ambience, 0, 10);
        this.brilliance = requireRange("brilliance", brilliance, -1, 1);
    }

    public PerformanceMode mode() {
        if (layerTone != null) {
            return PerformanceMode.DUAL;
        }
        if (leftTone != null) {
            return PerformanceMode.SPLIT;
        }
        return PerformanceMode.SINGLE;
    }

    public Tone tone(ToneRole role) {
        return switch (role) {
            case PRIMARY -> primaryTone;
            case LAYER -> layerTone;
            case LEFT -> leftTone;
        };
    }

    public PerformanceState assignSingle(Tone tone) {
        return withTones(Objects.requireNonNull(tone), null, null);
    }

    public PerformanceState assignPrimary(Tone tone) {
        return withTones(Objects.requireNonNull(tone), layerTone, leftTone);
    }

    public PerformanceState assignLayer(Tone tone) {
        return withTones(primaryTone, Objects.requireNonNull(tone), null);
    }

    public PerformanceState assignLeft(Tone tone) {
        return withTones(primaryTone, null, Objects.requireNonNull(tone));
    }

    public PerformanceState remove(ToneRole role, Tone fallbackTone) {
        Objects.requireNonNull(fallbackTone);
        return switch (role) {
            case LAYER -> withTones(primaryTone, null, leftTone);
            case LEFT -> withTones(primaryTone, layerTone, null);
            case PRIMARY -> removePrimary(fallbackTone);
        };
    }

    public PerformanceState withPerformanceSettings(
            int newBalance,
            int newSplitPoint,
            int newPrimaryOctaveShift,
            int newSecondaryOctaveShift,
            int newTranspose,
            int newKeyTouch,
            int newAmbience,
            int newBrilliance
    ) {
        return new PerformanceState(
                primaryTone,
                layerTone,
                leftTone,
                newBalance,
                newSplitPoint,
                newPrimaryOctaveShift,
                newSecondaryOctaveShift,
                newTranspose,
                newKeyTouch,
                newAmbience,
                newBrilliance
        );
    }

    private PerformanceState removePrimary(Tone fallbackTone) {
        if (layerTone != null) {
            return withTones(layerTone, null, null);
        }
        if (leftTone != null) {
            return withTones(leftTone, null, null);
        }
        return withTones(fallbackTone, null, null);
    }

    private PerformanceState withTones(Tone primary, Tone layer, Tone left) {
        return new PerformanceState(
                primary,
                layer,
                left,
                balance,
                splitPoint,
                primaryOctaveShift,
                secondaryOctaveShift,
                transpose,
                keyTouch,
                ambience,
                brilliance
        );
    }

    private static int requireRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be in " + minimum + ".." + maximum);
        }
        return value;
    }
}

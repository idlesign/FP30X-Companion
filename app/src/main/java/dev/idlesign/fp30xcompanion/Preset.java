package dev.idlesign.fp30xcompanion;

import java.util.Objects;

public final class Preset {
    public final String id;
    public final String name;
    public final PerformanceState state;
    public final long createdAtMillis;
    public final long updatedAtMillis;

    public Preset(
            String id,
            String name,
            PerformanceState state,
            long createdAtMillis,
            long updatedAtMillis
    ) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.state = Objects.requireNonNull(state);
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public Preset update(String newName, PerformanceState newState, long nowMillis) {
        return new Preset(id, newName, newState, createdAtMillis, nowMillis);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}

package dev.idlesign.fp30xcompanion;

public interface PerformanceOutput {
    boolean isConnected();

    void applyPerformanceState(PerformanceState state);

    default void applyMetronomeState(MetronomeState state) {
    }
}

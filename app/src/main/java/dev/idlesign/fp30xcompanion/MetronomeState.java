package dev.idlesign.fp30xcompanion;

public final class MetronomeState {
    public final boolean enabled;
    public final int tempo;
    public final int beat;
    public final int volume;

    public MetronomeState(boolean enabled, int tempo, int beat, int volume) {
        if (tempo < 10 || tempo > 500) {
            throw new IllegalArgumentException("tempo must be in 10..500");
        }
        if (beat < 0 || beat > 5) {
            throw new IllegalArgumentException("beat must be in 0..5");
        }
        if (volume < 0 || volume > 10) {
            throw new IllegalArgumentException("volume must be in 0..10");
        }
        this.enabled = enabled;
        this.tempo = tempo;
        this.beat = beat;
        this.volume = volume;
    }

    public static MetronomeState defaults() {
        return new MetronomeState(false, 120, 3, 5);
    }
}

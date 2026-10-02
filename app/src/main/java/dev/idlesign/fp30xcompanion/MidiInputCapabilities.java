package dev.idlesign.fp30xcompanion;

public final class MidiInputCapabilities {
    public final boolean available;
    public final int outputPortCount;

    public MidiInputCapabilities(boolean available, int outputPortCount) {
        this.available = available;
        this.outputPortCount = outputPortCount;
    }
}

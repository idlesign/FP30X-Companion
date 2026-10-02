package dev.idlesign.fp30xcompanion;

public final class RecordingPresentation {
    public final boolean midiEnabled;
    public final boolean audioEnabled;
    public final boolean disconnectEnabled;

    private RecordingPresentation(boolean midiEnabled, boolean audioEnabled, boolean disconnectEnabled) {
        this.midiEnabled = midiEnabled;
        this.audioEnabled = audioEnabled;
        this.disconnectEnabled = disconnectEnabled;
    }

    public static RecordingPresentation from(
            boolean connected,
            ConnectionTarget target,
            boolean midiInputAvailable,
            boolean usbAudioAvailable,
            boolean recording
    ) {
        return new RecordingPresentation(
                connected && midiInputAvailable && !recording,
                connected && target == ConnectionTarget.USB && usbAudioAvailable && !recording,
                connected && !recording
        );
    }
}

package dev.idlesign.fp30xcompanion;

public final class ConnectionBarState {
    public enum Phase {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    public final Phase phase;
    public final String message;

    public ConnectionBarState(Phase phase, String message) {
        this.phase = phase;
        this.message = message;
    }

    public boolean canChooseTarget() {
        return phase == Phase.DISCONNECTED || phase == Phase.ERROR;
    }

    public boolean canPressAction() {
        return phase != Phase.CONNECTING;
    }

    public boolean actionDisconnects() {
        return phase == Phase.CONNECTED;
    }
}

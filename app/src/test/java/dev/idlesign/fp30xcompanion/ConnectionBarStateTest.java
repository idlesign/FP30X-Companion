package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConnectionBarStateTest {
    @Test
    public void disconnectedAndErrorAllowTargetAndConnect() {
        assertConnectable(new ConnectionBarState(
                ConnectionBarState.Phase.DISCONNECTED, "Not connected"));
        assertConnectable(new ConnectionBarState(
                ConnectionBarState.Phase.ERROR, "Failed"));
    }

    @Test
    public void connectingDisablesTargetAndAction() {
        ConnectionBarState state = new ConnectionBarState(
                ConnectionBarState.Phase.CONNECTING, "Connecting");
        assertFalse(state.canChooseTarget());
        assertFalse(state.canPressAction());
        assertFalse(state.actionDisconnects());
    }

    @Test
    public void connectedOffersDisconnectAndLocksTarget() {
        ConnectionBarState state = new ConnectionBarState(
                ConnectionBarState.Phase.CONNECTED, "Connected");
        assertFalse(state.canChooseTarget());
        assertTrue(state.canPressAction());
        assertTrue(state.actionDisconnects());
    }

    private void assertConnectable(ConnectionBarState state) {
        assertTrue(state.canChooseTarget());
        assertTrue(state.canPressAction());
        assertFalse(state.actionDisconnects());
    }
}

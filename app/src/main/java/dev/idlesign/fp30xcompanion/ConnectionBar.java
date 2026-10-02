package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public final class ConnectionBar extends LinearLayout {
    public interface Listener {
        void onConnect(ConnectionTarget target);

        void onDisconnect();
    }

    private static final int COLOR_DARK = Color.rgb(18, 21, 26);
    private final Spinner targetSpinner;
    private final ImageButton actionButton;
    private final TextView recordingView;
    private final Listener listener;
    private final int baseBottomPadding;
    private ConnectionBarState state;
    private boolean recording;
    private Runnable recordingStop;

    public ConnectionBar(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(COLOR_DARK);
        int horizontal = dp(12);
        baseBottomPadding = dp(6);
        setPadding(horizontal, dp(4), horizontal, baseBottomPadding);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);

        recordingView = new TextView(context);
        recordingView.setTextSize(13);
        recordingView.setTextColor(Color.WHITE);
        recordingView.setGravity(Gravity.CENTER_VERTICAL);
        recordingView.setPadding(dp(8), 0, dp(8), 0);
        recordingView.setVisibility(GONE);
        controls.addView(recordingView, new LayoutParams(0, dp(40), 1));

        targetSpinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{context.getString(R.string.connection_bluetooth_midi),
                        context.getString(R.string.connection_usb)});
        targetSpinner.setAdapter(adapter);
        controls.addView(targetSpinner, new LayoutParams(0, dp(40), 1));

        actionButton = new ImageButton(context);
        actionButton.setImageResource(R.drawable.ic_connect_b);
        actionButton.setPadding(dp(8), dp(8), dp(8), dp(8));
        UiStyles.primaryIconButton(actionButton);
        actionButton.setContentDescription(context.getString(R.string.button_connect));
        actionButton.setOnClickListener(view -> onAction());
        LayoutParams actionParams = new LayoutParams(dp(44), dp(40));
        actionParams.setMargins(dp(8), 0, 0, 0);
        controls.addView(actionButton, actionParams);
        addView(controls, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setOnApplyWindowInsetsListener((view, insets) -> applyInsets(insets));
        showDisconnected(context.getString(R.string.status_not_connected));
    }

    public void showDisconnected(String message) {
        render(new ConnectionBarState(ConnectionBarState.Phase.DISCONNECTED, message));
    }

    public void showConnecting(String message) {
        render(new ConnectionBarState(ConnectionBarState.Phase.CONNECTING, message));
    }

    public void showConnected(String message) {
        render(new ConnectionBarState(ConnectionBarState.Phase.CONNECTED, message));
    }

    public void showError(String message) {
        render(new ConnectionBarState(ConnectionBarState.Phase.ERROR, message));
    }

    public void showRecording(String text, Runnable onStop) {
        recording = true;
        recordingStop = onStop;
        recordingView.setText(text);
        recordingView.setVisibility(VISIBLE);
        targetSpinner.setVisibility(GONE);
        actionButton.setEnabled(true);
        actionButton.setImageResource(R.drawable.ic_stop_b);
        actionButton.setContentDescription(getContext().getString(R.string.button_stop));
    }

    public void hideRecording(String connectedMessage) {
        recording = false;
        recordingStop = null;
        recordingView.setVisibility(GONE);
        targetSpinner.setVisibility(VISIBLE);
        showConnected(connectedMessage);
    }

    public ConnectionTarget selectedTarget() {
        return targetSpinner.getSelectedItemPosition() == 1
                ? ConnectionTarget.USB : ConnectionTarget.BLUETOOTH_MIDI;
    }

    private void render(ConnectionBarState newState) {
        state = newState;
        if (recording) {
            return;
        }
        targetSpinner.setEnabled(newState.canChooseTarget());
        actionButton.setEnabled(newState.canPressAction());
        boolean disconnect = newState.actionDisconnects();
        actionButton.setImageResource(disconnect
                ? R.drawable.ic_disconnect_b : R.drawable.ic_connect_b);
        actionButton.setContentDescription(getContext().getString(disconnect
                ? R.string.button_disconnect : R.string.button_connect));
    }

    private void onAction() {
        if (recording) {
            if (recordingStop != null) {
                recordingStop.run();
            }
            return;
        }
        if (state.actionDisconnects()) {
            listener.onDisconnect();
        } else {
            listener.onConnect(selectedTarget());
        }
    }

    private WindowInsets applyInsets(WindowInsets insets) {
        int bottom = insets == null ? 0 : insets.getSystemWindowInsetBottom();
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), baseBottomPadding + bottom);
        return insets;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

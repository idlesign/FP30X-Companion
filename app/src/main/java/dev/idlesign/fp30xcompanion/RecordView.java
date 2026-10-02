package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

public final class RecordView extends LinearLayout {
    public interface Listener {
        void onStart(boolean audio, boolean midi);

        void onStop();

        void onChangeDestination();
    }

    private final CheckBox audioCheck;
    private final CheckBox midiCheck;
    private final TextView audioHint;
    private final TextView destinationView;
    private final TextView timerView;
    private final ProgressBar levelView;
    private final Button recordButton;
    private final Listener listener;
    private boolean recording;

    public RecordView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setPadding(dp(16), dp(14), dp(16), dp(14));
        setBackgroundColor(Color.rgb(246, 247, 249));

        TextView title = text(getContext().getString(R.string.record_title), 22, Color.rgb(24, 28, 35));
        addView(title);

        audioCheck = new CheckBox(context);
        audioCheck.setText(R.string.record_audio);
        audioCheck.setChecked(true);
        addView(audioCheck);
        audioHint = text(getContext().getString(R.string.record_audio_usb_required),
                12, Color.rgb(103, 111, 124));
        audioHint.setPadding(dp(32), 0, 0, dp(8));
        addView(audioHint);

        midiCheck = new CheckBox(context);
        midiCheck.setText(R.string.record_midi);
        midiCheck.setChecked(true);
        addView(midiCheck);

        LinearLayout destination = new LinearLayout(context);
        destination.setGravity(Gravity.CENTER_VERTICAL);
        destination.setPadding(0, dp(16), 0, dp(8));
        destinationView = text(getContext().getString(R.string.recording_destination_downloads),
                15, Color.rgb(24, 28, 35));
        destination.addView(destinationView, new LayoutParams(0, dp(48), 1));
        Button change = new Button(context);
        change.setText(R.string.recording_destination_change);
        UiStyles.secondaryButton(change);
        change.setOnClickListener(view -> listener.onChangeDestination());
        destination.addView(change, new LayoutParams(dp(120), dp(48)));
        addView(destination);

        timerView = text("00:00.0", 32, Color.rgb(24, 28, 35));
        timerView.setGravity(Gravity.CENTER);
        addView(timerView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));

        levelView = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        levelView.setMax(1000);
        addView(levelView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));

        recordButton = new Button(context);
        recordButton.setText(R.string.button_record);
        recordButton.setTextSize(18);
        UiStyles.primaryButton(recordButton);
        recordButton.setOnClickListener(view -> {
            if (recording) {
                listener.onStop();
            } else {
                listener.onStart(audioCheck.isChecked(), midiCheck.isChecked());
            }
        });
        LayoutParams recordParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        recordParams.setMargins(0, dp(18), 0, 0);
        addView(recordButton, recordParams);
    }

    public void setDestination(RecordingDestination destination) {
        CharSequence label = destination.kind == RecordingDestination.Kind.DOWNLOADS
                ? getContext().getString(R.string.recording_destination_downloads)
                : destination.treeUri.getLastPathSegment();
        destinationView.setText(label);
    }

    public void setCapabilities(boolean connected, boolean midiAvailable, boolean audioAvailable) {
        if (recording) {
            return;
        }
        midiCheck.setEnabled(connected && midiAvailable);
        if (!midiCheck.isEnabled()) {
            midiCheck.setChecked(false);
        }
        audioCheck.setEnabled(connected && audioAvailable);
        if (!audioCheck.isEnabled()) {
            audioCheck.setChecked(false);
        }
        audioHint.setText(audioAvailable
                ? R.string.record_audio_usb_ready : R.string.record_audio_usb_required);
        updateRecordEnabled();
        audioCheck.setOnCheckedChangeListener((button, checked) -> updateRecordEnabled());
        midiCheck.setOnCheckedChangeListener((button, checked) -> updateRecordEnabled());
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
        audioCheck.setEnabled(!recording && audioCheck.isEnabled());
        midiCheck.setEnabled(!recording && midiCheck.isEnabled());
        recordButton.setText(recording ? R.string.button_stop : R.string.button_record);
        if (!recording) {
            timerView.setText("00:00.0");
            levelView.setProgress(0);
        }
    }

    public void setProgress(long elapsedMillis, float level) {
        long minutes = elapsedMillis / 60_000;
        double seconds = (elapsedMillis % 60_000) / 1000.0;
        timerView.setText(String.format(Locale.ROOT, "%02d:%04.1f", minutes, seconds));
        levelView.setProgress(Math.min(1000, Math.round(level * 1000)));
    }

    private void updateRecordEnabled() {
        recordButton.setEnabled(audioCheck.isChecked() || midiCheck.isChecked());
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

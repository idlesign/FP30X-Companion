package dev.idlesign.fp30xcompanion;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public final class MetronomeDialog {
    public interface Listener {
        void onPreview(MetronomeState state);
        void onApply(MetronomeState state, boolean changed);
        void onCancel();
    }

    private MetronomeDialog() {
    }

    public static void show(Context context, MetronomeState initial, Listener listener) {
        Editor editor = new Editor(context, initial, listener);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.metronome_settings_title)
                .setView(editor.build())
                .setNegativeButton(R.string.button_cancel, (ignored, which) -> editor.cancel())
                .setPositiveButton(R.string.button_done, (ignored, which) -> editor.apply())
                .create();
        dialog.setOnCancelListener(ignored -> editor.cancel());
        dialog.show();
        UiStyles.dialogButtons(dialog);
    }

    private static final class Editor {
        private final Context context;
        private final Listener listener;
        private MetronomeState state;
        private boolean changed;
        private boolean completed;

        Editor(Context context, MetronomeState state, Listener listener) {
            this.context = context;
            this.state = state;
            this.listener = listener;
        }

        View build() {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(16), dp(12), dp(16), dp(8));

            Switch enabled = new Switch(context);
            enabled.setText(R.string.setting_metronome_enabled);
            enabled.setChecked(state.enabled);
            enabled.setOnCheckedChangeListener((button, value) -> update(
                    value, state.tempo, state.beat, state.volume));
            root.addView(enabled, rowParams());

            slider(root, R.string.setting_tempo, 10, 500, state.tempo,
                    value -> update(state.enabled, value, state.beat, state.volume));
            spinner(root, R.string.setting_beat,
                    context.getResources().getStringArray(R.array.metronome_beats),
                    state.beat, value -> update(state.enabled, state.tempo, value, state.volume));
            slider(root, R.string.setting_metronome_volume, 0, 10, state.volume,
                    value -> update(state.enabled, state.tempo, state.beat, value));
            return root;
        }

        private void slider(LinearLayout parent, int labelId, int minimum, int maximum,
                            int current, IntListener changedListener) {
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text(context.getString(labelId), 13, Color.rgb(24, 28, 35)),
                    new LinearLayout.LayoutParams(dp(126), dp(44)));
            SeekBar seek = new SeekBar(context);
            seek.setMax(maximum - minimum);
            seek.setProgress(current - minimum);
            row.addView(seek, new LinearLayout.LayoutParams(0, dp(44), 1));
            TextView value = text(Integer.toString(current), 13, Color.rgb(103, 111, 124));
            value.setGravity(Gravity.CENTER);
            row.addView(value, new LinearLayout.LayoutParams(dp(54), dp(44)));
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    value.setText(Integer.toString(minimum + progress));
                }
                @Override public void onStartTrackingTouch(SeekBar bar) { }
                @Override public void onStopTrackingTouch(SeekBar bar) {
                    changedListener.changed(minimum + bar.getProgress());
                }
            });
            parent.addView(row, rowParams());
        }

        private void spinner(LinearLayout parent, int labelId, String[] values,
                             int current, IntListener changedListener) {
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text(context.getString(labelId), 13, Color.rgb(24, 28, 35)),
                    new LinearLayout.LayoutParams(dp(126), dp(44)));
            Spinner spinner = new Spinner(context);
            spinner.setAdapter(new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_dropdown_item, values));
            spinner.setSelection(current, false);
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                private boolean first = true;
                @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                                     View view, int position, long id) {
                    if (first && position == current) {
                        first = false;
                        return;
                    }
                    first = false;
                    changedListener.changed(position);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            row.addView(spinner, new LinearLayout.LayoutParams(0, dp(44), 1));
            parent.addView(row, rowParams());
        }

        private void update(boolean enabled, int tempo, int beat, int volume) {
            state = new MetronomeState(enabled, tempo, beat, volume);
            changed = true;
            listener.onPreview(state);
        }

        void apply() {
            completed = true;
            listener.onApply(state, changed);
        }

        void cancel() {
            if (!completed) {
                completed = true;
                listener.onCancel();
            }
        }

        private LinearLayout.LayoutParams rowParams() {
            return new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private TextView text(String value, int size, int color) {
            TextView view = new TextView(context);
            view.setText(value);
            view.setTextSize(size);
            view.setTextColor(color);
            view.setGravity(Gravity.CENTER_VERTICAL);
            return view;
        }

        private int dp(int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }

    private interface IntListener {
        void changed(int value);
    }
}

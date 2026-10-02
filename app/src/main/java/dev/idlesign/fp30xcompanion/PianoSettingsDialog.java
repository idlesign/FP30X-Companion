package dev.idlesign.fp30xcompanion;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public final class PianoSettingsDialog {
    public interface Listener {
        void onPerformancePreview(PerformanceState state);
        void onApply(PerformanceState state, boolean performanceChanged);
        void onCancel();
    }

    private PianoSettingsDialog() {
    }

    public static void show(
            Context context,
            PerformanceState initial,
            Listener listener
    ) {
        Editor editor = new Editor(context, initial, listener);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.piano_settings_title)
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
        private PerformanceState state;
        private boolean performanceChanged;
        private boolean completed;

        Editor(Context context, PerformanceState state, Listener listener) {
            this.context = context;
            this.state = state;
            this.listener = listener;
        }

        View build() {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(12), dp(12), dp(12), dp(6));
            root.addView(performancePage(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return root;
        }

        private LinearLayout performancePage() {
            LinearLayout page = page();
            section(page, R.string.settings_general_section);
            slider(page, R.string.setting_transpose, -6, 5, state.transpose,
                    true, PianoSettingsDialog::signed,
                    value -> updatePerformance(state.balance, state.splitPoint,
                            state.primaryOctaveShift, state.secondaryOctaveShift, value,
                            state.keyTouch, state.ambience, state.brilliance));
            spinner(page, R.string.setting_key_touch,
                    context.getResources().getStringArray(R.array.key_touch_values),
                    state.keyTouch, value -> updatePerformance(state.balance, state.splitPoint,
                            state.primaryOctaveShift, state.secondaryOctaveShift, state.transpose,
                            value, state.ambience, state.brilliance));
            slider(page, R.string.setting_ambience, 0, 10, state.ambience,
                    true, null, value -> updatePerformance(state.balance, state.splitPoint,
                            state.primaryOctaveShift, state.secondaryOctaveShift, state.transpose,
                            state.keyTouch, value, state.brilliance));
            slider(page, R.string.setting_brilliance, -1, 1, state.brilliance,
                    true, PianoSettingsDialog::signed,
                    value -> updatePerformance(state.balance, state.splitPoint,
                            state.primaryOctaveShift, state.secondaryOctaveShift, state.transpose,
                            state.keyTouch, state.ambience, value));

            if (state.mode() == PerformanceMode.DUAL) {
                section(page, R.string.mode_dual);
                slider(page, R.string.setting_balance, 0, 18, state.balance,
                        true, null, value -> updatePerformance(value, state.splitPoint,
                                state.primaryOctaveShift, state.secondaryOctaveShift,
                                state.transpose, state.keyTouch, state.ambience, state.brilliance));
                section(page, R.string.role_main);
                slider(page, R.string.setting_octave, -3, 3, state.primaryOctaveShift,
                        true, PianoSettingsDialog::signed, value -> updatePerformance(
                                state.balance, state.splitPoint, value,
                                state.secondaryOctaveShift, state.transpose, state.keyTouch,
                                state.ambience, state.brilliance));
                section(page, R.string.role_layer);
                slider(page, R.string.setting_octave, -3, 3, state.secondaryOctaveShift,
                        true, PianoSettingsDialog::signed, value -> updatePerformance(
                                state.balance, state.splitPoint, state.primaryOctaveShift,
                                value, state.transpose, state.keyTouch,
                                state.ambience, state.brilliance));
            } else if (state.mode() == PerformanceMode.SPLIT) {
                section(page, R.string.mode_split);
                slider(page, R.string.setting_split_point, 0, 127, state.splitPoint,
                        true, PianoSettingsDialog::noteName,
                        value -> updatePerformance(state.balance, value,
                                state.primaryOctaveShift, state.secondaryOctaveShift,
                                state.transpose, state.keyTouch, state.ambience, state.brilliance));
                slider(page, R.string.setting_balance, 0, 18, state.balance,
                        true, null, value -> updatePerformance(value, state.splitPoint,
                                state.primaryOctaveShift, state.secondaryOctaveShift,
                                state.transpose, state.keyTouch, state.ambience, state.brilliance));
                section(page, R.string.role_left);
                slider(page, R.string.setting_octave, -3, 3, state.secondaryOctaveShift,
                        true, PianoSettingsDialog::signed, value -> updatePerformance(
                                state.balance, state.splitPoint, state.primaryOctaveShift,
                                value, state.transpose, state.keyTouch,
                                state.ambience, state.brilliance));
                section(page, R.string.role_right);
                slider(page, R.string.setting_octave, -3, 3, state.primaryOctaveShift,
                        true, PianoSettingsDialog::signed, value -> updatePerformance(
                                state.balance, state.splitPoint, value,
                                state.secondaryOctaveShift, state.transpose, state.keyTouch,
                                state.ambience, state.brilliance));
            }
            return page;
        }

        private void section(LinearLayout parent, int labelId) {
            TextView heading = text(context.getString(labelId), 13, Color.rgb(230, 106, 44));
            heading.setTypeface(Typeface.DEFAULT_BOLD);
            heading.setPadding(0, dp(7), 0, 0);
            parent.addView(heading, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(29)));
        }

        private void slider(
                LinearLayout parent, int labelId, int minimum, int maximum, int current,
                boolean enabled, ValueFormatter formatter, IntListener changed
        ) {
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(context.getString(labelId), 13, Color.rgb(24, 28, 35));
            row.addView(label, new LinearLayout.LayoutParams(dp(126), dp(40)));
            SeekBar seek = new SeekBar(context);
            seek.setMax(maximum - minimum);
            seek.setProgress(current - minimum);
            seek.setEnabled(enabled);
            row.addView(seek, new LinearLayout.LayoutParams(0, dp(40), 1));
            TextView value = text(format(current, formatter), 13, Color.rgb(103, 111, 124));
            value.setGravity(Gravity.CENTER);
            row.addView(value, new LinearLayout.LayoutParams(dp(54), dp(40)));
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    value.setText(format(minimum + progress, formatter));
                }
                @Override public void onStartTrackingTouch(SeekBar bar) { }
                @Override public void onStopTrackingTouch(SeekBar bar) {
                    changed.changed(minimum + bar.getProgress());
                }
            });
            parent.addView(row, rowParams());
        }

        private void spinner(LinearLayout parent, int labelId, String[] values,
                             int current, IntListener changed) {
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text(context.getString(labelId), 13, Color.rgb(24, 28, 35)),
                    new LinearLayout.LayoutParams(dp(126), dp(40)));
            Spinner spinner = new Spinner(context);
            spinner.setAdapter(new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_dropdown_item, values));
            spinner.setSelection(current, false);
            spinner.setOnItemSelectedListener(new SimpleItemSelectedListener(current, changed));
            row.addView(spinner, new LinearLayout.LayoutParams(0, dp(40), 1));
            parent.addView(row, rowParams());
        }

        private void updatePerformance(int balance, int splitPoint, int primaryOctave,
                                       int secondaryOctave, int transpose, int keyTouch,
                                       int ambience, int brilliance) {
            state = state.withPerformanceSettings(balance, splitPoint, primaryOctave,
                    secondaryOctave, transpose, keyTouch, ambience, brilliance);
            performanceChanged = true;
            listener.onPerformancePreview(state);
        }

        void apply() {
            completed = true;
            listener.onApply(state, performanceChanged);
        }

        void cancel() {
            if (!completed) {
                completed = true;
                listener.onCancel();
            }
        }

        private LinearLayout page() {
            LinearLayout page = new LinearLayout(context);
            page.setOrientation(LinearLayout.VERTICAL);
            return page;
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

    private interface IntListener { void changed(int value); }
    private interface ValueFormatter { String format(int value); }

    private static final class SimpleItemSelectedListener
            implements android.widget.AdapterView.OnItemSelectedListener {
        private final int initial;
        private final IntListener listener;
        private boolean first = true;
        SimpleItemSelectedListener(int initial, IntListener listener) {
            this.initial = initial;
            this.listener = listener;
        }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                             int position, long id) {
            if (first && position == initial) {
                first = false;
                return;
            }
            first = false;
            listener.changed(position);
        }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }

    private static String format(int value, ValueFormatter formatter) {
        return formatter == null ? Integer.toString(value) : formatter.format(value);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    static int pickerIndex(int minimum, int logicalValue) {
        return logicalValue - minimum;
    }

    static int pickerValue(int minimum, int pickerIndex) {
        return minimum + pickerIndex;
    }

    static String noteName(int midiNote) {
        String[] names = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B"};
        return names[midiNote % 12] + (midiNote / 12 - 1);
    }
}

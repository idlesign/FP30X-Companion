package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

public final class PianoView extends LinearLayout {
    public interface Listener {
        void onChooseTone(ToneRole role);
        void onSave();
        void onPresets();
        void onSettings();
        void onMetronome();
        void onRemove(ToneRole role);
    }

    private static final int COLOR_INK = Color.rgb(24, 28, 35);
    private static final int COLOR_MUTED = Color.rgb(103, 111, 124);
    private static final int COLOR_ORANGE = Color.rgb(230, 106, 44);
    private final TextView modeView;
    private final TextView presetView;
    private final MiniKeyboardView keyboardView;
    private final LinearLayout settingsTable;
    private final LinearLayout metronomeTable;
    private final Listener listener;

    public PianoView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(8), dp(12), dp(8));
        setBackgroundColor(Color.rgb(246, 247, 249));

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        modeView = text("", 21, COLOR_INK);
        modeView.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(modeView, new LayoutParams(0, dp(40), 1));
        ImageButton save = iconButton(
                R.drawable.ic_save_b, R.string.action_save_preset, listener::onSave);
        titleRow.addView(save, new LayoutParams(dp(42), dp(42)));
        Space actionGap = new Space(context);
        titleRow.addView(actionGap, new LayoutParams(dp(12), dp(1)));
        ImageButton presets = iconButton(
                R.drawable.ic_presets_b, R.string.action_presets, listener::onPresets);
        titleRow.addView(presets, new LayoutParams(dp(42), dp(42)));
        addView(titleRow);

        presetView = text("", 14, COLOR_MUTED);
        presetView.setPadding(0, 0, 0, dp(4));
        addView(presetView);

        keyboardView = new MiniKeyboardView(context, new MiniKeyboardView.Listener() {
            @Override public void onTone(ToneRole role) { listener.onChooseTone(role); }
            @Override public void onRemove(ToneRole role) { listener.onRemove(role); }
        });
        addView(keyboardView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space spacer = new Space(context);
        addView(spacer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        settingsTable = new LinearLayout(context);
        settingsTable.setOrientation(VERTICAL);
        settingsTable.setPadding(dp(10), dp(5), dp(10), dp(5));
        UiStyles.surface(settingsTable, Color.rgb(235, 237, 241), 10);
        settingsTable.setContentDescription(getContext().getString(R.string.piano_settings));
        settingsTable.setOnClickListener(view -> listener.onSettings());
        addView(settingsTable, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        metronomeTable = new LinearLayout(context);
        metronomeTable.setOrientation(VERTICAL);
        metronomeTable.setPadding(dp(10), dp(5), dp(10), dp(5));
        UiStyles.surface(metronomeTable, Color.rgb(226, 229, 234), 10);
        metronomeTable.setContentDescription(
                getContext().getString(R.string.metronome_settings_title));
        metronomeTable.setOnClickListener(view -> listener.onMetronome());
        LayoutParams metronomeParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metronomeParams.setMargins(0, dp(7), 0, 0);
        addView(metronomeTable, metronomeParams);
    }

    public void render(
            PerformanceState state,
            Preset preset,
            boolean dirty,
            MetronomeState metronome
    ) {
        modeView.setText(modeLabel(state.mode()));
        presetView.setText(preset.name + (dirty ? " •" : ""));
        boolean primaryRemovalChangesState = !(state.mode() == PerformanceMode.SINGLE
                && "Concert Piano".equals(state.primaryTone.name));
        keyboardView.bind(state, primaryRemovalChangesState);
        renderSettings(state);
        renderMetronome(metronome);
    }

    private void renderSettings(PerformanceState state) {
        settingsTable.removeAllViews();
        addSetting(settingsTable, R.string.setting_transpose, signed(state.transpose));
        String touch = getResources().getStringArray(R.array.key_touch_values)[state.keyTouch];
        addSetting(settingsTable, R.string.setting_key_touch, touch);
        addSetting(settingsTable, R.string.setting_ambience, Integer.toString(state.ambience));
        addSetting(settingsTable, R.string.setting_brilliance, signed(state.brilliance));
        if (state.mode() == PerformanceMode.DUAL) {
            addSetting(settingsTable, R.string.setting_balance, Integer.toString(state.balance));
            addSetting(settingsTable, R.string.setting_primary_octave, signed(state.primaryOctaveShift));
            addSetting(settingsTable, R.string.setting_secondary_octave, signed(state.secondaryOctaveShift));
        } else if (state.mode() == PerformanceMode.SPLIT) {
            addSetting(settingsTable, R.string.setting_split_point, PianoSettingsDialog.noteName(state.splitPoint));
            addSetting(settingsTable, R.string.setting_balance, Integer.toString(state.balance));
            addSetting(settingsTable, R.string.setting_left_octave, signed(state.secondaryOctaveShift));
            addSetting(settingsTable, R.string.setting_right_octave, signed(state.primaryOctaveShift));
        }
    }

    private void renderMetronome(MetronomeState metronome) {
        metronomeTable.removeAllViews();
        TextView title = text(getContext().getString(R.string.settings_metronome_section),
                12, COLOR_ORANGE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        metronomeTable.addView(title, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));
        addSetting(metronomeTable, R.string.setting_metronome_state,
                getContext().getString(metronome.enabled
                        ? R.string.value_on : R.string.value_off));
        addSetting(metronomeTable, R.string.setting_tempo,
                Integer.toString(metronome.tempo));
        String beat = getResources().getStringArray(R.array.metronome_beats)[metronome.beat];
        addSetting(metronomeTable, R.string.setting_beat, beat);
        addSetting(metronomeTable, R.string.setting_metronome_volume,
                Integer.toString(metronome.volume));
    }

    private void addSetting(LinearLayout table, int labelId, String value) {
        LinearLayout row = new LinearLayout(getContext());
        TextView key = text(getContext().getString(labelId), 11, COLOR_MUTED);
        TextView valueView = text(value, 11, COLOR_INK);
        valueView.setGravity(Gravity.END);
        row.addView(key, new LayoutParams(0, dp(19), 1));
        row.addView(valueView, new LayoutParams(dp(110), dp(19)));
        table.addView(row);
    }

    private ImageButton iconButton(int icon, int description, Runnable action) {
        return UiIconButton.create(getContext(), icon, description, COLOR_ORANGE, action);
    }

    private String modeLabel(PerformanceMode mode) {
        return switch (mode) {
            case SINGLE -> getContext().getString(R.string.mode_single);
            case DUAL -> getContext().getString(R.string.mode_dual);
            case SPLIT -> getContext().getString(R.string.mode_split);
        };
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

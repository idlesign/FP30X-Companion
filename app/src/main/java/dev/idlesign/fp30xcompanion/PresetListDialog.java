package dev.idlesign.fp30xcompanion;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class PresetListDialog extends Dialog {
    public interface Listener {
        List<Preset> presets();

        void onLoad(String id);

        void onDuplicate(String id);

        void onDelete(String id);
    }

    private final Listener listener;
    private LinearLayout rows;

    public PresetListDialog(Context context, Listener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(buildContent());
        refresh();
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private LinearLayout buildContent() {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        SafeArea.applyAll(root, dp(12), dp(10), dp(12), dp(10));
        root.setBackgroundColor(Color.rgb(246, 247, 249));

        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(getContext().getString(R.string.action_presets), 22, Color.rgb(24, 28, 35));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button close = new Button(getContext());
        close.setText(R.string.button_done);
        UiStyles.secondaryButton(close);
        close.setOnClickListener(view -> dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(100), dp(48)));
        root.addView(header);

        ScrollView scroll = new ScrollView(getContext());
        rows = new LinearLayout(getContext());
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        scrollParams.topMargin = dp(12);
        root.addView(scroll, scrollParams);
        return root;
    }

    private void refresh() {
        if (rows == null) {
            return;
        }
        rows.removeAllViews();
        for (Preset preset : listener.presets()) {
            rows.addView(presetRow(preset));
        }
        if (rows.getChildCount() == 0) {
            TextView empty = text(getContext().getString(R.string.presets_empty), 16,
                    Color.rgb(103, 111, 124));
            empty.setGravity(Gravity.CENTER);
            rows.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
        }
    }

    private LinearLayout presetRow(Preset preset) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(6), dp(4), dp(6));
        UiStyles.surface(row, Color.WHITE, 12);

        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(preset.name, 16, Color.rgb(24, 28, 35));
        TextView summary = text(summary(preset), 12, Color.rgb(103, 111, 124));
        labels.addView(name);
        labels.addView(summary);
        row.addView(labels, new LinearLayout.LayoutParams(0, dp(58), 1));

        ImageButton actions = UiIconButton.create(
                getContext(), R.drawable.ic_more_b, R.string.button_actions,
                Color.rgb(80, 87, 98), () -> { });
        actions.setOnClickListener(view -> showPresetActions(actions, preset));
        row.addView(actions, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return row;
    }

    private String summary(Preset preset) {
        PerformanceState state = preset.state;
        String tones = state.primaryTone.name;
        if (state.layerTone != null) {
            tones += " + " + state.layerTone.name;
        } else if (state.leftTone != null) {
            tones = state.leftTone.name + " | " + tones;
        }
        String created = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(preset.createdAtMillis));
        return state.mode().name() + " · " + tones + " · " + created;
    }

    private void showPresetActions(ImageButton anchor, Preset preset) {
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add(0, 1, 0, R.string.action_load_preset);
        menu.getMenu().add(0, 2, 1, R.string.action_duplicate_preset);
        menu.getMenu().add(0, 3, 2, R.string.action_delete_preset);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                listener.onLoad(preset.id);
                dismiss();
            } else if (item.getItemId() == 2) {
                listener.onDuplicate(preset.id);
                refresh();
            } else if (item.getItemId() == 3) {
                listener.onDelete(preset.id);
                refresh();
            } else {
                return false;
            }
            return true;
        });
        menu.show();
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}

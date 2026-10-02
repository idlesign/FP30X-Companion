package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class ToneRowView extends LinearLayout {
    private static final int COLOR_INK = Color.rgb(24, 28, 35);
    private static final int COLOR_MUTED = Color.rgb(103, 111, 124);
    private static final int COLOR_ORANGE = Color.rgb(230, 106, 44);
    private final TextView nameView;
    private final TextView detailsView;
    private final Button favoriteButton;
    private final Button actionButton;

    ToneRowView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(12), dp(7), dp(8), dp(7));
        setBackgroundColor(Color.WHITE);

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(VERTICAL);
        nameView = text(16, COLOR_INK);
        detailsView = text(12, COLOR_MUTED);
        labels.addView(nameView);
        labels.addView(detailsView);
        addView(labels, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        favoriteButton = button();
        addView(favoriteButton, new LayoutParams(dp(74), dp(48)));
        actionButton = button();
        addView(actionButton, new LayoutParams(dp(82), dp(48)));
    }

    public static ToneRowView assigned(
            Context context,
            Tone tone,
            String role,
            boolean removable,
            Runnable onRemove
    ) {
        ToneRowView row = new ToneRowView(context);
        row.nameView.setText(tone.name);
        row.nameView.setTypeface(Typeface.DEFAULT_BOLD);
        row.detailsView.setText(role.toUpperCase());
        row.detailsView.setTextColor(COLOR_ORANGE);
        row.favoriteButton.setVisibility(View.GONE);
        row.actionButton.setText("🗑");
        row.actionButton.setTextSize(20);
        UiStyles.ghostButton(row.actionButton, COLOR_MUTED);
        row.actionButton.setContentDescription(context.getString(R.string.action_remove_assignment));
        row.actionButton.setOnClickListener(view -> onRemove.run());
        row.actionButton.setVisibility(removable ? View.VISIBLE : View.GONE);
        return row;
    }

    public void bindCatalog(
            Tone tone,
            String details,
            String assignments,
            boolean favorite,
            Runnable onFavorite,
            Runnable onAssign
    ) {
        nameView.setText(tone.name);
        nameView.setTypeface(Typeface.DEFAULT, assignments.isEmpty() ? Typeface.NORMAL : Typeface.BOLD);
        nameView.setTextColor(assignments.isEmpty() ? COLOR_INK : COLOR_ORANGE);
        detailsView.setText(assignments.isEmpty() ? details : assignments + "  ·  " + details);
        favoriteButton.setVisibility(View.VISIBLE);
        favoriteButton.setText(favorite ? "★" : "☆");
        favoriteButton.setTextSize(24);
        UiStyles.ghostButton(favoriteButton, favorite ? COLOR_ORANGE : COLOR_MUTED);
        favoriteButton.setTextColor(favorite ? COLOR_ORANGE : COLOR_MUTED);
        favoriteButton.setContentDescription(getContext().getString(R.string.category_favorites));
        favoriteButton.setOnClickListener(view -> onFavorite.run());
        actionButton.setText(assignments.isEmpty() ? "…" : assignments);
        actionButton.setTextSize(12);
        actionButton.setAllCaps(false);
        UiStyles.ghostButton(actionButton, COLOR_ORANGE);
        actionButton.setContentDescription(getContext().getString(R.string.action_assign_tone));
        actionButton.setOnClickListener(view -> onAssign.run());
    }

    public Button actionButton() {
        return actionButton;
    }

    private Button button() {
        Button button = new Button(getContext());
        button.setTextSize(18);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        return button;
    }

    private TextView text(int size, int color) {
        TextView view = new TextView(getContext());
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

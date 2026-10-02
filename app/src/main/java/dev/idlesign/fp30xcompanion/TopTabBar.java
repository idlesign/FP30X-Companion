package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public final class TopTabBar extends LinearLayout {
    public interface Listener {
        void onTabSelected(int index);
    }

    private static final int COLOR_DARK = Color.rgb(18, 21, 26);
    private static final int COLOR_INACTIVE = Color.rgb(170, 176, 186);
    private final View[] buttons = new View[3];
    private final int baseHorizontalPadding;
    private final int baseTopPadding;
    private final int baseBottomPadding;
    private int selectedIndex;

    public TopTabBar(Context context, Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(COLOR_DARK);
        baseHorizontalPadding = dp(8);
        baseTopPadding = dp(2);
        baseBottomPadding = dp(2);
        setPadding(baseHorizontalPadding, baseTopPadding,
                baseHorizontalPadding, baseBottomPadding);
        setOnApplyWindowInsetsListener((view, insets) -> applySafeInsets(insets));
        buttons[0] = addTextTab(R.string.tab_piano, 0, listener);
        buttons[1] = addTextTab(R.string.tab_record, 1, listener);
        buttons[2] = addInfoTab(2, listener);
        select(0);
    }

    public void select(int index) {
        selectedIndex = index;
        for (int current = 0; current < buttons.length; current++) {
            boolean selected = current == index;
            View view = buttons[current];
            UiStyles.tab(view, selected);
            if (view instanceof Button button) {
                button.setTextColor(selected ? Color.WHITE : COLOR_INACTIVE);
                button.setTypeface(Typeface.DEFAULT,
                        selected ? Typeface.BOLD : Typeface.NORMAL);
            } else if (view instanceof ImageButton button) {
                button.setImageTintList(ColorStateList.valueOf(
                        selected ? Color.WHITE : COLOR_INACTIVE));
            }
        }
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    private WindowInsets applySafeInsets(WindowInsets insets) {
        int top = insets == null ? 0 : insets.getSystemWindowInsetTop();
        int left = insets == null ? 0 : insets.getSystemWindowInsetLeft();
        int right = insets == null ? 0 : insets.getSystemWindowInsetRight();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && insets != null) {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                top = Math.max(top, cutout.getSafeInsetTop());
                left = Math.max(left, cutout.getSafeInsetLeft());
                right = Math.max(right, cutout.getSafeInsetRight());
            }
        }
        setPadding(baseHorizontalPadding + left, baseTopPadding + top,
                baseHorizontalPadding + right, baseBottomPadding);
        return insets;
    }

    private Button addTextTab(int text, int index, Listener listener) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setAllCaps(true);
        button.setTextSize(13);
        button.setMinHeight(dp(36));
        button.setOnClickListener(view -> selectAndNotify(index, listener));
        addView(button, new LayoutParams(0, dp(44), 1));
        return button;
    }

    private ImageButton addInfoTab(int index, Listener listener) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(R.drawable.ic_help_b);
        button.setContentDescription(getContext().getString(R.string.tab_info));
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setOnClickListener(view -> selectAndNotify(index, listener));
        LayoutParams params = new LayoutParams(dp(56), dp(44));
        params.setMargins(dp(6), 0, 0, 0);
        addView(button, params);
        return button;
    }

    private void selectAndNotify(int index, Listener listener) {
        select(index);
        listener.onTabSelected(index);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

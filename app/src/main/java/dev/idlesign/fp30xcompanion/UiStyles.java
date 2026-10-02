package dev.idlesign.fp30xcompanion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

public final class UiStyles {
    private static final int COLOR_INK = Color.rgb(24, 28, 35);
    private static final int COLOR_ORANGE = Color.rgb(230, 106, 44);
    private static final int COLOR_ORANGE_PRESSED = Color.rgb(197, 79, 28);
    private static final int COLOR_DISABLED = Color.rgb(157, 163, 173);
    private static final int COLOR_LIGHT = Color.rgb(235, 237, 241);
    private static final int COLOR_LIGHT_PRESSED = Color.rgb(215, 219, 226);
    private static final int COLOR_GHOST_PRESSED = Color.argb(36, 230, 106, 44);

    private UiStyles() {
    }

    public static void dialogButtons(AlertDialog dialog) {
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (negative != null) {
            secondaryButton(negative);
            addDialogButtonSpacing(negative);
        }
        if (positive != null) {
            primaryButton(positive);
            addDialogButtonSpacing(positive);
        }
        if (neutral != null) {
            secondaryButton(neutral);
            addDialogButtonSpacing(neutral);
        }
    }

    public static void primaryButton(Button button) {
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackground(states(button.getContext(), COLOR_ORANGE,
                COLOR_ORANGE_PRESSED, COLOR_DISABLED, 10));
    }

    public static void secondaryButton(Button button) {
        button.setAllCaps(false);
        button.setTextColor(COLOR_INK);
        button.setBackground(states(button.getContext(), COLOR_LIGHT,
                COLOR_LIGHT_PRESSED, Color.rgb(229, 231, 235), 10));
    }

    public static void ghostButton(Button button, int textColor) {
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setBackground(states(button.getContext(), Color.TRANSPARENT,
                COLOR_GHOST_PRESSED, Color.TRANSPARENT, 10));
    }

    public static void iconButton(ImageButton button, int tint) {
        button.setImageTintList(ColorStateList.valueOf(tint));
        button.setBackground(states(button.getContext(), Color.argb(24, 230, 106, 44),
                Color.argb(54, 230, 106, 44), Color.TRANSPARENT, 10));
    }

    public static void primaryIconButton(ImageButton button) {
        button.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        button.setBackground(states(button.getContext(), COLOR_ORANGE,
                COLOR_ORANGE_PRESSED, COLOR_DISABLED, 10));
    }

    public static void tab(View view, boolean selected) {
        int normal = selected ? COLOR_ORANGE : Color.TRANSPARENT;
        int pressed = selected ? COLOR_ORANGE_PRESSED : Color.rgb(48, 53, 62);
        view.setBackground(states(view.getContext(), normal, pressed,
                Color.TRANSPARENT, 9));
    }

    public static void surface(View view, int color, int radiusDp) {
        view.setBackground(shape(view.getContext(), color, radiusDp));
    }

    private static StateListDrawable states(
            Context context,
            int normal,
            int pressed,
            int disabled,
            int radiusDp
    ) {
        StateListDrawable result = new StateListDrawable();
        result.addState(new int[]{-android.R.attr.state_enabled},
                shape(context, disabled, radiusDp));
        result.addState(new int[]{android.R.attr.state_pressed},
                shape(context, pressed, radiusDp));
        result.addState(new int[]{}, shape(context, normal, radiusDp));
        return result;
    }

    private static GradientDrawable shape(Context context, int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    private static void addDialogButtonSpacing(Button button) {
        ViewGroup.LayoutParams params = button.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams margins) {
            int horizontalMargin = dp(button.getContext(), 4);
            margins.setMarginStart(horizontalMargin);
            margins.setMarginEnd(horizontalMargin);
            button.setLayoutParams(margins);
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

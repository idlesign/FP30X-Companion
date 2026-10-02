package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.widget.ImageButton;

public final class UiIconButton {
    private UiIconButton() {
    }

    public static ImageButton create(
            Context context,
            int drawable,
            int description,
            int tint,
            Runnable action
    ) {
        ImageButton button = new ImageButton(context);
        button.setImageResource(drawable);
        button.setContentDescription(context.getString(description));
        button.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        UiStyles.iconButton(button, tint);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

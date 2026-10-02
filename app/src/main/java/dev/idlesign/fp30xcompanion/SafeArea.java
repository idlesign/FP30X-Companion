package dev.idlesign.fp30xcompanion;

import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

public final class SafeArea {
    private SafeArea() {
    }

    public static void applyAll(View view, int left, int top, int right, int bottom) {
        view.setPadding(left, top, right, bottom);
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            int safeLeft = insets == null ? 0 : insets.getSystemWindowInsetLeft();
            int safeTop = insets == null ? 0 : insets.getSystemWindowInsetTop();
            int safeRight = insets == null ? 0 : insets.getSystemWindowInsetRight();
            int safeBottom = insets == null ? 0 : insets.getSystemWindowInsetBottom();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && insets != null) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    safeLeft = Math.max(safeLeft, cutout.getSafeInsetLeft());
                    safeTop = Math.max(safeTop, cutout.getSafeInsetTop());
                    safeRight = Math.max(safeRight, cutout.getSafeInsetRight());
                    safeBottom = Math.max(safeBottom, cutout.getSafeInsetBottom());
                }
            }
            target.setPadding(left + safeLeft, top + safeTop,
                    right + safeRight, bottom + safeBottom);
            return insets;
        });
    }
}

package dev.idlesign.fp30xcompanion;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class InfoView extends LinearLayout {
    public static final String REPOSITORY_URL = "https://github.com/idlesign/FP30X-Companion";

    private static final int COLOR_INK = Color.rgb(24, 28, 35);
    private static final int COLOR_MUTED = Color.rgb(103, 111, 124);
    private static final int COLOR_ORANGE = Color.rgb(230, 106, 44);

    public InfoView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(16), dp(18), dp(16), dp(18));
        setBackgroundColor(Color.rgb(246, 247, 249));

        TextView title = text(getContext().getString(R.string.info_title), 22, COLOR_INK);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        addView(title, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        addView(infoRow(
                getContext().getString(R.string.info_version),
                versionName(context),
                false
        ), rowParams());
        addView(infoRow(
                getContext().getString(R.string.info_repository),
                REPOSITORY_URL,
                true
        ), rowParams());
    }

    private LinearLayout infoRow(String label, String value, boolean link) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        UiStyles.surface(row, Color.WHITE, 14);

        TextView labelView = text(label, 12, COLOR_MUTED);
        row.addView(labelView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

        TextView valueView = text(value, link ? 14 : 16, link ? COLOR_ORANGE : COLOR_INK);
        if (link) {
            valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            valueView.setContentDescription(getContext().getString(R.string.info_repository_link));
            valueView.setOnClickListener(view -> openRepository());
        }
        row.addView(valueView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private LayoutParams rowParams() {
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        return params;
    }

    private void openRepository() {
        Intent intent = repositoryBrowserIntent();
        if (!(getContext() instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(getContext(), R.string.error_no_browser, Toast.LENGTH_LONG).show();
        }
    }

    static Intent repositoryBrowserIntent() {
        Intent intent = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER);
        intent.setData(Uri.parse(REPOSITORY_URL));
        return intent;
    }

    @SuppressWarnings("deprecation")
    private static String versionName(Context context) {
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            }
            return info.versionName == null ? "—" : info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "—";
        }
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

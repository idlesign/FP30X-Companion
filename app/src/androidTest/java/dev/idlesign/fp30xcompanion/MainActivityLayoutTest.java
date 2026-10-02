package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Insets;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.SdkSuppress;

import org.junit.Rule;
import org.junit.Test;

public class MainActivityLayoutTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void topTabsAndBottomConnectionBarExist() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(findView(activity.getWindow().getDecorView(), TopTabBar.class));
            ConnectionBar bar = (ConnectionBar) findView(
                    activity.getWindow().getDecorView(), ConnectionBar.class);
            assertNotNull(bar);
            assertEquals(1, bar.getChildCount());
        });
    }

    @Test
    public void pianoSummaryBlocksAreAnchoredAtTheBottom() {
        activityRule.getScenario().onActivity(activity -> {
            PianoView piano = (PianoView) findView(
                    activity.getWindow().getDecorView(), PianoView.class);
            assertNotNull(piano);
            View metronome = findViewWithDescription(
                    piano, activity.getString(R.string.metronome_settings_title));
            assertNotNull(metronome);
            assertEquals(piano.getHeight() - piano.getPaddingBottom(), metronome.getBottom());
        });
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void connectionBarAddsNavigationInset() {
        activityRule.getScenario().onActivity(activity -> {
            ConnectionBar bar = (ConnectionBar) findView(
                    activity.getWindow().getDecorView(), ConnectionBar.class);
            assertNotNull(bar);
            int before = bar.getPaddingBottom();
            WindowInsets insets = new WindowInsets.Builder()
                    .setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, 42))
                    .build();
            bar.dispatchApplyWindowInsets(insets);
            assertEquals(before + 42, bar.getPaddingBottom());
        });
    }

    private View findView(View root, Class<?> type) {
        if (type.isInstance(root)) {
            return root;
        }
        if (root instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                View result = findView(group.getChildAt(index), type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private View findViewWithDescription(View root, String description) {
        if (description.equals(root.getContentDescription())) {
            return root;
        }
        if (root instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                View result = findViewWithDescription(group.getChildAt(index), description);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}

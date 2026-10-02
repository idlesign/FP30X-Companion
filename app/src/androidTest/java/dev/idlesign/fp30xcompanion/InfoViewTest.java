package dev.idlesign.fp30xcompanion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public final class InfoViewTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void infoTabShowsVersionAndRepository() {
        AtomicReference<String> version = new AtomicReference<>();
        activityRule.getScenario().onActivity(activity -> {
            try {
                version.set(activity.getPackageManager()
                        .getPackageInfo(activity.getPackageName(), 0)
                        .versionName);
            } catch (PackageManager.NameNotFoundException error) {
                throw new AssertionError(error);
            }
        });

        onView(withContentDescription(R.string.tab_info)).perform(click());
        onView(withText(InfoView.REPOSITORY_URL)).check(matches(isDisplayed()));
        onView(withText(version.get())).check(matches(isDisplayed()));
    }
    @Test
    public void repositoryUsesBrowserIntent() {
        Intent intent = InfoView.repositoryBrowserIntent();
        assertEquals(Intent.ACTION_MAIN, intent.getAction());
        assertEquals(Uri.parse(InfoView.REPOSITORY_URL), intent.getData());
        assertNotNull(intent.getSelector());
        assertTrue(intent.getSelector().hasCategory(Intent.CATEGORY_APP_BROWSER));
    }

}

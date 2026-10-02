package dev.idlesign.fp30xcompanion;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;

@RunWith(AndroidJUnit4.class)
public final class PianoSettingsDialogTest {
    @Test
    public void settingsEntryOpensCompactDialog() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withContentDescription(R.string.piano_settings)).perform(click());
            onView(withText(R.string.piano_settings_title)).check(matches(isDisplayed()));
        }
    }
}

package dev.idlesign.fp30xcompanion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

public class PresetDialogTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void saveDialogShowsCurrentNameAndCancel() {
        onView(withContentDescription(R.string.action_save_preset)).perform(click());
        onView(withText(R.string.preset_save_title)).check(matches(isDisplayed()));
        onView(withText(R.string.default_profile_name)).check(matches(isDisplayed()));
        onView(withText(R.string.button_cancel)).perform(click());
    }

    @Test
    public void presetsButtonOpensPresetList() {
        onView(withContentDescription(R.string.action_presets)).perform(click());
        onView(withText(R.string.action_presets)).check(matches(isDisplayed()));
        onView(withText(R.string.button_done)).perform(click());
    }
}

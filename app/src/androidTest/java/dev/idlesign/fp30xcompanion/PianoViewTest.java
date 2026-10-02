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

public class PianoViewTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void chooseTonesOpensCatalogAndDoneClosesIt() {
        onView(withContentDescription(R.string.action_choose_tones)).perform(click());
        onView(withText(R.string.button_done)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.mode_single)).check(matches(isDisplayed()));
    }
}

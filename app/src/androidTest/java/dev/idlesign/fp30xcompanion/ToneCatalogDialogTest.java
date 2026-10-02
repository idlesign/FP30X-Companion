package dev.idlesign.fp30xcompanion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class ToneCatalogDialogTest {
    @Test
    public void filterDropdownShowsGroupedFamilies() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withContentDescription(R.string.action_choose_tones)).perform(click());
            onView(withContentDescription(R.string.tone_filter_content_description)).perform(click());
            onView(withText(R.string.filter_section_quick)).check(matches(isDisplayed()));
            onView(withText(R.string.category_favorites)).check(matches(isDisplayed()));
            onView(withText(R.string.filter_section_keyboards)).check(matches(isDisplayed()));
            onView(withText(R.string.filter_section_sound_effects)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void filterAndSearchAreRestoredWhenCatalogIsReopened() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withContentDescription(R.string.action_choose_tones)).perform(click());
            onView(withHint(R.string.search_hint))
                    .perform(androidx.test.espresso.action.ViewActions.typeText("guitar"));
            onView(withContentDescription(R.string.tone_filter_content_description)).perform(click());
            onView(withText(R.string.filter_guitars)).perform(click());
            onView(withText(R.string.button_done)).perform(click());

            onView(withContentDescription(R.string.action_choose_tones)).perform(click());
            onView(withHint(R.string.search_hint)).check(matches(withText("guitar")));
            onView(withContentDescription(R.string.tone_filter_content_description))
                    .check(matches(withSpinnerText(R.string.filter_guitars)));
        }
    }
}

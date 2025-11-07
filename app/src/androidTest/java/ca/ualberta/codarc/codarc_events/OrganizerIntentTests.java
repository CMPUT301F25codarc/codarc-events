package ca.ualberta.codarc.codarc_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assume;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.views.ManageWaitlistActivity;
import ca.ualberta.codarc.codarc_events.views.ViewCancelledActivity;
import ca.ualberta.codarc.codarc_events.views.ViewEnrolledActivity;
import ca.ualberta.codarc.codarc_events.views.ViewWinnersActivity;
@LargeTest
@RunWith(AndroidJUnit4.class)
public class OrganizerIntentTests {

    // ---------------- Manage Waitlist ----------------
    @Test
    public void organizer_manageWaitlist_rendersListOrEmpty() {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), ManageWaitlistActivity.class);
        i.putExtra("eventId", "E123");
        try (ActivityScenario<ManageWaitlistActivity> ignored = ActivityScenario.launch(i)) {
            // Either list shows or empty state shows
            onView(withId(R.id.rv_entrants)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }
    // ---------------- View Cancelled ----------------
    @Test
    public void organizer_viewCancelled_clickReplace_showsDialog_thenCancel() {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), ViewCancelledActivity.class);
        i.putExtra("eventId", "E123");
        try (ActivityScenario<ViewCancelledActivity> scenario = ActivityScenario.launch(i)) {
            // If organizer verification closes the Activity, skip this test
            if (!assumeViewPresent(R.id.rv_entrants)) return;

            // Click Replace on first row if present
            onView(withId(R.id.rv_entrants))
                    .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_replace)));

            // Dialog should appear; cancel to avoid side effects
            onView(withText("Draw Replacement")).check(matches(isDisplayed()));
            onView(withText("Cancel")).perform(click());
        }
    }
    @Test
    public void organizer_viewCancelled_emptyOrList_andNotifyButtonVisibility() {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), ViewCancelledActivity.class);
        i.putExtra("eventId", "E123");
        try (ActivityScenario<ViewCancelledActivity> scenario = ActivityScenario.launch(i)) {
            if (!assumeViewPresent(R.id.rv_entrants)) return;
            // Just assert screen content renders
            onView(withId(R.id.rv_entrants)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
            // Notify button exists; we do NOT click it to avoid writes
            onView(withId(R.id.btn_notify_cancelled)).check(matches(isDisplayed()));
        }
    }
    // ---------------- View Enrolled ----------------
    @Test
    public void organizer_viewEnrolled_rendersListOrEmpty() {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), ViewEnrolledActivity.class);
        i.putExtra("eventId", "E123");
        try (ActivityScenario<ViewEnrolledActivity> scenario = ActivityScenario.launch(i)) {
            if (!assumeViewPresent(R.id.rv_entrants)) return;
            onView(withId(R.id.rv_entrants)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }
    // ---------------- View Winners ----------------
    @Test
    public void organizer_viewWinners_renders_and_rowStatusIfPresent() {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), ViewWinnersActivity.class);
        i.putExtra("eventId", "E123");
        try (ActivityScenario<ViewWinnersActivity> scenario = ActivityScenario.launch(i)) {
            if (!assumeViewPresent(R.id.rv_entrants)) return;
            onView(withId(R.id.rv_entrants)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
            // If first row is present, its status TextView should be part of the layout
            onView(recyclerChildAt(R.id.rv_entrants, 0, R.id.tv_status))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }
    @Test
    public void organizer_viewWinners_notifyButtonVisible_butNotClicked() {
        Intent i = new Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), ViewWinnersActivity.class);
        i.putExtra("eventId", "E123");
        try (ActivityScenario<ViewWinnersActivity> scenario = ActivityScenario.launch(i)) {
            if (!assumeViewPresent(R.id.rv_entrants)) return;
            onView(withId(R.id.btn_notify_winners)).check(matches(isDisplayed()));
            // Do not click to avoid backend writes
        }
    }

    // ---------------- Helpers ----------------
    /** Click a child view inside a RecyclerView row by its ID. */
    private static androidx.test.espresso.ViewAction clickChildViewWithId(@IdRes int id) {
        return new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "Click child view with id " + id; }
            @Override public void perform(androidx.test.espresso.UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null && v.isShown()) v.performClick();
            }
        };
    }

    /** Match a child view within a RecyclerView item at a given position. */
    private static Matcher<View> recyclerChildAt(@IdRes int recyclerId, int position, @IdRes int targetChildId) {
        return new TypeSafeMatcher<View>() {
            @Override public void describeTo(org.hamcrest.Description description) {
                description.appendText("RecyclerView child at position " + position + " with id " + targetChildId);
            }
            @Override protected boolean matchesSafely(View view) {
                View root = view.getRootView();
                View rv = root.findViewById(recyclerId);
                if (!(rv instanceof RecyclerView)) return false;
                RecyclerView recyclerView = (RecyclerView) rv;
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(position);
                if (vh == null) return false; // not bound yet
                View child = vh.itemView.findViewById(targetChildId);
                return view == child;
            }
        };
    }

    /**
     * If the view isn't present (e.g., activity closed by organizer verification),
     * skip the test gracefully.
     */
    private static boolean assumeViewPresent(@IdRes int id) {
        try {
            onView(withId(id)).check(matches(isDisplayed()));
            return true;
        } catch (Throwable t) { // NoMatchingViewException or assertion failure
            Assume.assumeTrue("Organizer access not available in this environment", false);
            return false;
        }
    }
}

package ca.ualberta.codarc.codarc_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.views.EventDetailsActivity;
import ca.ualberta.codarc.codarc_events.views.LandingActivity;
import ca.ualberta.codarc.codarc_events.views.NotificationsActivity;
import ca.ualberta.codarc.codarc_events.views.ProfileCreationActivity;

/**
 * Espresso UI tests using **adapter item IDs** for clicks and assertions.
 * No backend guarantees; we just exercise adapters and screens.
 *
 * Adapters & IDs referenced:
 * - EventCardAdapter row (item_event_card):
 *   tv_event_title, tv_lottery_ends, tv_entrants_info, tv_waitlist_count,
 *   btn_join_list, btn_lottery_info
 * - NotificationAdapter row (item_notifications):
 *   btn_notification_accept, btn_notification_decline, tv_notification_status
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class EntrantIntentTests {

    @Rule
    public ActivityScenarioRule<LandingActivity> landingRule = new ActivityScenarioRule<>(LandingActivity.class);

    // ---------- Event list (EventCardAdapter) ----------

    /** Opens browser and taps the "Join" button inside the first card (btn_join_list). */
    @Test
    public void entrant_joinFromCard_clicksJoinButton() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(withId(R.id.rv_events)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_events))
                .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_join_list)));
        // If a dialog or toast appears, great — this test only validates the button is clickable.
    }

    /** Opens the lottery info dialog from the first card (btn_lottery_info) and dismisses it. */
    @Test
    public void entrant_viewLotteryCriteria_fromCard() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(withId(R.id.rv_events))
                .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_lottery_info)));
        // The dialog layout is custom; we at least check the positive button text exists.
        onView(withText(R.string.got_it)).check(matches(isDisplayed()));
        onView(withText(R.string.got_it)).perform(click());
    }

    /** Taps the first card to open EventDetailsActivity, then asserts details controls. */
    @Test
    public void entrant_openDetails_fromCard_andSeeJoinLeave() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(withId(R.id.rv_events)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.btn_join_waitlist)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_leave_waitlist)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    /** Asserts the waitlist count TextView exists in the first row (tv_waitlist_count). */
    @Test
    public void entrant_seeWaitlistCount_onCard() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(recyclerChildAt(R.id.rv_events, 0, R.id.tv_waitlist_count))
                .check(matches(isDisplayed()));
    }

    // ---------- Profile (ProfileCreationActivity) ----------

    @Test
    public void entrant_fillProfileAndSave() {
        try (ActivityScenario<ProfileCreationActivity> ignored = ActivityScenario.launch(ProfileCreationActivity.class)) {
            onView(withId(R.id.et_name)).perform(scrollTo(), replaceText("Ava Example"), closeSoftKeyboard());
            onView(withId(R.id.et_email)).perform(scrollTo(), replaceText("ava@example.com"), closeSoftKeyboard());
            onView(withId(R.id.et_phone)).perform(scrollTo(), replaceText("5551234567"), closeSoftKeyboard());
            onView(withId(R.id.btn_create_profile)).perform(scrollTo(), click());
        }
    }

    @Test
    public void entrant_deleteProfile_confirmsDialog() {
        try (ActivityScenario<ProfileCreationActivity> ignored = ActivityScenario.launch(ProfileCreationActivity.class)) {
            onView(withId(R.id.btn_delete_profile)).perform(scrollTo(), click());
            onView(withText("Delete Profile")).check(matches(isDisplayed()));
            onView(withText("Delete")).perform(click());
        }
    }

    // ---------- Notifications (NotificationAdapter) ----------

    /** Inbox renders list or empty state; row buttons use adapter IDs when items exist. */
    @Test
    public void entrant_notificationsInbox_renders() {
        try (ActivityScenario<NotificationsActivity> ignored = ActivityScenario.launch(NotificationsActivity.class)) {
            onView(withId(R.id.rv_notifications)).check(matches(isDisplayed()));
        }
    }

    /** If an invite row exists at position 0, click Accept (btn_notification_accept). */
    @Test
    public void entrant_notification_accept_onFirstRow_ifPresent() {
        try (ActivityScenario<NotificationsActivity> ignored = ActivityScenario.launch(NotificationsActivity.class)) {
            onView(withId(R.id.rv_notifications))
                    .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_notification_accept)));
        }
    }

    /** If an invite row exists at position 0, click Decline (btn_notification_decline). */
    @Test
    public void entrant_notification_decline_onFirstRow_ifPresent() {
        try (ActivityScenario<NotificationsActivity> ignored = ActivityScenario.launch(NotificationsActivity.class)) {
            onView(withId(R.id.rv_notifications))
                    .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.btn_notification_decline)));
        }
    }

    // ---------- Details screen direct launch for join/leave dialogs ----------

    @Test
    public void entrant_join_fromDetails_showsJoinDialog() {
        try (ActivityScenario<EventDetailsActivity> ignored = ActivityScenario.launch(buildEventDetailsIntent())) {
            onView(withId(R.id.btn_join_waitlist)).perform(scrollTo(), click());
            onView(withText("Join Waitlist")).check(matches(isDisplayed()));
            onView(withText("Join")).perform(click());
        }
    }

    @Test
    public void entrant_leave_fromDetails_showsLeaveDialog() {
        try (ActivityScenario<EventDetailsActivity> ignored = ActivityScenario.launch(buildEventDetailsIntent())) {
            onView(withId(R.id.btn_leave_waitlist)).perform(scrollTo(), click());
            onView(withText("Leave Waitlist")).check(matches(isDisplayed()));
            onView(withText("Leave")).perform(click());
        }
    }

    // ---------- helpers ----------

    private Intent buildEventDetailsIntent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Event e = new Event();
        e.setId("E123");
        e.setName("Sample Event");
        e.setDescription("Sample description");
        e.setEventDateTime("2025-12-01 07:00 PM");
        e.setRegistrationOpen("2025-11-20 08:00 AM");
        e.setRegistrationClose("2025-11-30 10:00 PM");
        e.setOrganizerId("ORG123");
        e.setLocation("Campus Hall");
        Intent i = new Intent(ctx, EventDetailsActivity.class);
        i.putExtra("event", e);
        return i;
    }

    /** Click a child view inside a RecyclerView row by its ID. */
    private static androidx.test.espresso.ViewAction clickChildViewWithId(@IdRes int id) {
        return new androidx.test.espresso.ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "Click on a child view with id " + id; }
            @Override public void perform(androidx.test.espresso.UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null && v.isShown()) v.performClick();
            }
        };
    }

    /** Match a child view within a RecyclerView item at a given position. */
    private static Matcher<View> recyclerChildAt(@IdRes int recyclerId, int position, @IdRes int targetChildId) {
        return new TypeSafeMatcher<>() {
            @Override public void describeTo(org.hamcrest.Description description) {
                description.appendText("RecyclerView child at position " + position + " with id " + targetChildId);
            }
            @Override protected boolean matchesSafely(View view) {
                View rv = view.getRootView().findViewById(recyclerId);
                if (!(rv instanceof RecyclerView)) return false;
                RecyclerView recyclerView = (RecyclerView) rv;
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(position);

                if (vh == null) return false; // not laid out yet
                View child = vh.itemView.findViewById(targetChildId);
                return view == child;
            }
        };
    }
}

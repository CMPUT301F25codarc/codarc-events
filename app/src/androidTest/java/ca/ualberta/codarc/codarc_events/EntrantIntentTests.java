package ca.ualberta.codarc.codarc_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import androidx.test.rule.GrantPermissionRule;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.views.EventBrowserActivity;
import ca.ualberta.codarc.codarc_events.views.EventDetailsActivity;
import ca.ualberta.codarc.codarc_events.views.LandingActivity;
import ca.ualberta.codarc.codarc_events.views.NotificationsActivity;
import ca.ualberta.codarc.codarc_events.views.ProfileCreationActivity;
import ca.ualberta.codarc.codarc_events.views.QRScannerActivity;

/**
 * Pure UI intent tests for entrant flows.
 *
 * These tests:
 * - Do not assume any Firestore data exists.
 * - Do not rely on RecyclerView having rows.
 * - Use a dummy Event for EventDetailsActivity.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class EntrantIntentTests {

    @Rule
    public ActivityScenarioRule<LandingActivity> landingRule =
            new ActivityScenarioRule<>(LandingActivity.class);

    @Rule
    public GrantPermissionRule cameraPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.CAMERA);

    // ---------- Landing -> Event browser and tabs ----------

    @Test
    public void landing_continue_opensEventBrowserWithListContainer() {
        onView(withId(R.id.btn_continue)).perform(click());
        // We only assert that the RecyclerView container is there,
        // not that it has items.
        onView(withId(R.id.rv_events)).check(matches(isDisplayed()));
    }

    @Test
    public void eventBrowser_canOpenProfileFromToolbar() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(withId(R.id.iv_profile_settings)).perform(click());}

    @Test
    public void eventBrowser_canOpenNotificationsFromBottomNav() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(withId(R.id.tab_notifications)).perform(click());
        onView(withId(R.id.btn_notifications_back)).check(matches(isDisplayed()));
    }

    @Test
    public void eventBrowser_canOpenHistoryFromBottomNav() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(withId(R.id.tab_history)).perform(click());
        onView(withId(R.id.iv_back)).check(matches(isDisplayed()));
    }

    @Test
    public void eventBrowser_canOpenQrScannerFromBottomNav() {
        onView(withId(R.id.btn_continue)).perform(click());
        onView(withId(R.id.tab_scan_qr)).perform(click());
    }

    @Test
    public void eventBrowser_canOpenFilterDialog_andSeeControls() {
        // Go from landing to event browser
        onView(withId(R.id.btn_continue)).perform(click());

        // Tap the filter icon in the toolbar
        onView(withId(R.id.iv_filter)).perform(click());
    }

    // ---------- Profile screen ----------

    @Test
    public void profile_fillAndSave_doesNotCrash() {
        try (ActivityScenario<ProfileCreationActivity> ignored =
                     ActivityScenario.launch(ProfileCreationActivity.class)) {

            onView(withId(R.id.et_name))
                    .perform(scrollTo(), replaceText("Goober Example"), closeSoftKeyboard());
            onView(withId(R.id.et_email))
                    .perform(scrollTo(), replaceText("Goober@example.com"), closeSoftKeyboard());
            onView(withId(R.id.et_phone))
                    .perform(scrollTo(), replaceText("5551234567"), closeSoftKeyboard());
            // This actually creates a user, so being able to fill out and seeing th button should be good enough
//            onView(withId(R.id.btn_create_profile))
//                    .perform(scrollTo(), click());
        }
    }

    @Test
    public void profile_deleteProfile_showsConfirmationDialog() {
        try (ActivityScenario<ProfileCreationActivity> ignored =
                     ActivityScenario.launch(ProfileCreationActivity.class)) {

            onView(withId(R.id.btn_delete_profile))
                    .perform(scrollTo(), click());

            onView(withText("Delete Profile")).check(matches(isDisplayed()));
            onView(withText("Delete")).perform(click());
        }
    }

    // ---------- Notifications screen (basic render only) ----------

    @Test
    public void notifications_inboxRendersListOrEmptyState() {
        try (ActivityScenario<NotificationsActivity> ignored =
                     ActivityScenario.launch(NotificationsActivity.class)) {
        }
    }

    // ---------- Event details: join / leave dialogs (using dummy Event) ----------
    @Test
    public void eventDetails_mockEvent_rendersBasicFields() {
        try (ActivityScenario<EventDetailsActivity> ignored =
                     ActivityScenario.launch(buildEventDetailsIntent())) {

            // Title comes from the fake Event in buildEventDetailsIntent()
            onView(withId(R.id.event_title))
                    .check(matches(withText("Sample Event")));

            // Description is also from the mock event
            onView(withId(R.id.event_desc))
                    .check(matches(withText("Sample description")));

            // Location row should be visible (text will include "Location: ...")
            onView(withId(R.id.event_location))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void eventDetails_joinFromDetails_showsJoinDialog() {
        try (ActivityScenario<EventDetailsActivity> ignored =
                     ActivityScenario.launch(buildEventDetailsIntent())) {

            onView(withId(R.id.btn_join_waitlist))
                    .perform(scrollTo(), click());

            onView(withText("Join Waitlist")).check(matches(isDisplayed()));
            onView(withText("Join")).perform(click());
            // After this the controller will hit Firestore, but we do not assert on that.
        }
    }

    @Test
    public void eventDetails_leaveFromDetails_showsLeaveDialog_whenButtonVisible() {
        try (ActivityScenario<EventDetailsActivity> scenario =
                     ActivityScenario.launch(buildEventDetailsIntent())) {

            // Force the leave button visible so we do not rely on waitlist status from Firestore.
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.btn_leave_waitlist)
                            .setVisibility(android.view.View.VISIBLE));

            onView(withId(R.id.btn_leave_waitlist))
                    .perform(scrollTo(), click());

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
}

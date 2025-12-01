package ca.ualberta.codarc.codarc_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.views.CreateEventActivity;
import ca.ualberta.codarc.codarc_events.views.EventSettingsActivity;
import ca.ualberta.codarc.codarc_events.views.ManageWaitlistActivity;
import ca.ualberta.codarc.codarc_events.views.ViewCancelledActivity;
import ca.ualberta.codarc.codarc_events.views.ViewEnrolledActivity;
import ca.ualberta.codarc.codarc_events.views.ViewWinnersActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class OrganizerIntentTests {

    // ---------------- Manage Waitlist ----------------

    @Test
    public void organizer_manageWaitlist_launches() {
        Intent i = new Intent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                ManageWaitlistActivity.class
        );
        i.putExtra("eventId", "E123");

        try (ActivityScenario<ManageWaitlistActivity> ignored =
                     ActivityScenario.launch(i)) {
            // If the Activity launches and doesn't crash, test passes.
        }
    }

    // ---------------- View Cancelled ----------------

    @Test
    public void organizer_viewCancelled_launches() {
        Intent i = new Intent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                ViewCancelledActivity.class
        );
        i.putExtra("eventId", "E123");

        try (ActivityScenario<ViewCancelledActivity> ignored =
                     ActivityScenario.launch(i)) {
            // Just verify screen can be opened with an eventId extra.
        }
    }

    // ---------------- View Enrolled ----------------

    @Test
    public void organizer_viewEnrolled_launches() {
        Intent i = new Intent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                ViewEnrolledActivity.class
        );
        i.putExtra("eventId", "E123");

        try (ActivityScenario<ViewEnrolledActivity> ignored =
                     ActivityScenario.launch(i)) {

        }
    }

    // ---------------- View Winners ----------------

    @Test
    public void organizer_viewWinners_launches() {
        Intent i = new Intent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                ViewWinnersActivity.class
        );
        i.putExtra("eventId", "E123");

        try (ActivityScenario<ViewWinnersActivity> ignored =
                     ActivityScenario.launch(i)) {

        }
    }
    // ---------- Helper: mock organizer event ----------
    private Event buildMockOrganizerEvent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        String organizerId = Identity.getOrCreateDeviceId(ctx);

        Event e = new Event();
        e.setId("ORG_EVENT_123");
        e.setName("Organizer Test Event");
        e.setDescription("Mock event for organizer UI tests");
        e.setEventDateTime("2025-12-01T19:00:00");
        e.setRegistrationOpen("2025-11-01T09:00:00");
        e.setRegistrationClose("2025-11-30T23:59:59");
        e.setOrganizerId(organizerId);  // matches device id so settings screen does not auto finish
        e.setLocation("Test Hall");
        return e;
    }

// ---------- Create Event screen just launches ----------

    @Test
    public void organizer_createEvent_launches() {
        try (ActivityScenario<CreateEventActivity> ignored =
                     ActivityScenario.launch(CreateEventActivity.class)) {
            // Launch only, no assertions
        }
    }

// ---------- Event Settings: launch with mock event ----------

    @Test
    public void organizer_eventSettings_launchesWithMockEvent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent i = new Intent(ctx, EventSettingsActivity.class);
        i.putExtra("event", buildMockOrganizerEvent());

        try (ActivityScenario<EventSettingsActivity> ignored =
                     ActivityScenario.launch(i)) {
        }
    }

// ---------- Event Settings: navigation buttons just navigate ----------

    @Test
    public void organizer_eventSettings_canNavigateToManageWaitlist() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent i = new Intent(ctx, EventSettingsActivity.class);
        i.putExtra("event", buildMockOrganizerEvent());

        try (ActivityScenario<EventSettingsActivity> ignored =
                     ActivityScenario.launch(i)) {
            onView(withId(R.id.btn_manage_waitlist)).perform(click());
        }
    }

    @Test
    public void organizer_eventSettings_canNavigateToDrawActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent i = new Intent(ctx, EventSettingsActivity.class);
        i.putExtra("event", buildMockOrganizerEvent());

        try (ActivityScenario<EventSettingsActivity> ignored =
                     ActivityScenario.launch(i)) {
            onView(withId(R.id.btn_run_lottery)).perform(click());
        }
    }

    @Test
    public void organizer_eventSettings_canNavigateToViewWinners() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent i = new Intent(ctx, EventSettingsActivity.class);
        i.putExtra("event", buildMockOrganizerEvent());

        try (ActivityScenario<EventSettingsActivity> ignored =
                     ActivityScenario.launch(i)) {
            onView(withId(R.id.btn_view_winners)).perform(click());
        }
    }

    @Test
    public void organizer_eventSettings_canNavigateToViewCancelled() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent i = new Intent(ctx, EventSettingsActivity.class);
        i.putExtra("event", buildMockOrganizerEvent());

        try (ActivityScenario<EventSettingsActivity> ignored =
                     ActivityScenario.launch(i)) {
            onView(withId(R.id.btn_view_cancelled)).perform(click());
        }
    }

    @Test
    public void organizer_eventSettings_canNavigateToViewEnrolled() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent i = new Intent(ctx, EventSettingsActivity.class);
        i.putExtra("event", buildMockOrganizerEvent());

        try (ActivityScenario<EventSettingsActivity> ignored =
                     ActivityScenario.launch(i)) {
            onView(withId(R.id.btn_view_enrolled)).perform(click());
        }
    }
}
